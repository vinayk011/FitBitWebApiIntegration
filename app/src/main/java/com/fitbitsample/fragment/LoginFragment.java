package com.fitbitsample.fragment;


import android.arch.lifecycle.Observer;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.fitbitsample.activity.MainActivity;
import com.fitbitsample.constant.PrefConstants;
import com.fitbitsample.R;
import com.fitbitsample.preference.AppPreference;
import com.fitbitsample.util.Trace;
import com.fitbitsample.viewmodel.GetAccessTokenModel;


public class LoginFragment extends BaseFragment {


    Boolean haveToken;
    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    private CustomTabsClient mClient;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private CustomTabsIntent customTabsIntent;
    private Button logInButton;


    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setRetainInstance(true);
        resources = getResources();
        context = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        logInButton = (Button) view.findViewById(R.id.button);
        return view;

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        speedUpChromeTabs();
        CustomTabsClient.bindCustomTabsService(getActivity(), CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection);
        customTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                .setToolbarColor(ContextCompat.getColor(getActivity(), R.color.primary))
                .setShowTitle(true)
                .build();
        haveToken = AppPreference.getInstance().getBoolean(PrefConstants.HAVE_AUTHORIZATION, false);

        /** client_id, which is provided by fitbit while registering app
            redirect_uri, provided by you when register app in fitbit.
        */
        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!haveToken) {
                    String url = "https://www.fitbit.com/oauth2/authorize?" +
                            "response_type=code" +
                            "&client_id=XXXXXX" +
                            "&expires_in=86400" +
                            "&scope=activity%20nutrition%20heartrate%20location%20nutrition%20profile%20settings%20sleep%20social%20weight" +
                            "&redirect_uri=hugfit://logincallback" +
                            "&prompt=login";
                    customTabsIntent.launchUrl(getActivity(), Uri.parse(url));
                } else {
                    Toast.makeText(getActivity(),
                            "Already logged in. Please go Back", Toast.LENGTH_SHORT).show();
                }
            }
        });
        verifyAuthCode();
    }

    private void verifyAuthCode() {
        if (AppPreference.getInstance().getBoolean(PrefConstants.IS_CODE_RECEIVED) && !haveToken) {
            GetAccessTokenModel accessTokenModel = new GetAccessTokenModel(2);
            accessTokenModel.run(context, null).getData().observe(this, new Observer<Integer>() {
                @Override
                public void onChanged(@Nullable Integer integer) {
                    if (integer != null && integer > 0) {
                        Trace.i("AccessToken fetching failed");
                    } else {
                        Trace.i("Access Token fetching is done");
                        showDashboard();
                    }
                }
            });
        }
    }

    private void showDashboard() {
        ((MainActivity) context).showDashboard();
    }

    private void speedUpChromeTabs() {
        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                //Pre-warming
                mClient = customTabsClient;
                mClient.warmup(0L);
                mCustomTabsSession = mClient.newSession(null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
            }
        };
    }


    public void resume() {
        if (getUserVisibleHint()) {
            ((MainActivity) context).setTitle(getString(R.string.login));

        }
        if (!haveToken) {
            getView().setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        getFragmentManager().popBackStack();
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}

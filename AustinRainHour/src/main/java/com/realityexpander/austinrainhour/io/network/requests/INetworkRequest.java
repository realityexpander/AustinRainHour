package com.realityexpander.austinrainhour.io.network.requests;

import android.net.Uri;
import com.realityexpander.austinrainhour.io.utilities.NetworkUtils;

/**
 * Created by Pequots34 on 7/31/13.
 */
public interface INetworkRequest<T> {

    public Uri.Builder getUri();

    public NetworkUtils.Method getMethod();

    public Class<? extends T> getResponse();

    public String getPostBody();

    public String getContentType();

}

package com.mercadopago.android.px.internal.viewmodel.drawables;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import java.io.Serializable;

public abstract class DrawableFragmentItem implements Serializable {

    protected String id;

    public abstract Fragment draw(@NonNull final PaymentMethodFragmentDrawer drawer);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
package com.mercadopago.android.px.internal.features.express.slider;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mercadolibre.android.card.header.model.CardHeaderView;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.internal.util.ResourceUtil;
import com.mercadopago.android.px.internal.viewmodel.drawables.SavedCardDrawableFragmentItem;
import com.mercadopago.android.px.model.PaymentTypes;

public class SavedCardFragment extends PaymentMethodFragment {

    @NonNull
    public static Fragment getInstance(final SavedCardDrawableFragmentItem savedCard) {
        final SavedCardFragment savedCardFragment = new SavedCardFragment();
        final Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_MODEL, savedCard);
        bundle.putString(ARG_PM_TYPE, PaymentTypes.CREDIT_CARD);
        savedCardFragment.setArguments(bundle);
        return savedCardFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.px_fragment_saved_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_MODEL)) {
            final SavedCardDrawableFragmentItem drawableCard =
                (SavedCardDrawableFragmentItem) arguments.getSerializable(ARG_MODEL);

            final CardHeaderView cardView = view.findViewById(R.id.card);

            setIssuerIcon(view, drawableCard);
            setPaymentMethodIcon(view, drawableCard);

            cardView.getCard().setName(drawableCard.card.getName());
            cardView.getCard().setExpiration(drawableCard.card.getDate());
            cardView.getCard().setNumber(drawableCard.card.getNumber());
            cardView.show(drawableCard.card);
        } else {
            throw new IllegalStateException("SavedCardFragment does not contain card information");
        }
    }

    protected void setIssuerIcon(@NonNull final View view, @NonNull final SavedCardDrawableFragmentItem drawableCard) {
        final int issuerResource = ResourceUtil.getCardIssuerImage(view.getContext(),
            drawableCard.card.getIssuerImageName());

        if (issuerResource > 0) {
            drawableCard.card.setIssuerRes(issuerResource);
        }
    }

    private void setPaymentMethodIcon(@NonNull final View view,
        @NonNull final SavedCardDrawableFragmentItem drawableCard) {
        final int paymentMethodResource = ResourceUtil.getCardImage(view.getContext(),
            drawableCard.paymentMethodId);

        if (paymentMethodResource > 0) {
            drawableCard.card.setLogoRes(paymentMethodResource);
        }
    }
}

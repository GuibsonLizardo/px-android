package com.mercadopago.android.px.internal.features.payer_information;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mercadopago.android.px.internal.base.BasePresenter;
import com.mercadopago.android.px.internal.callbacks.FailureRecovery;
import com.mercadopago.android.px.internal.callbacks.TaggedCallback;
import com.mercadopago.android.px.internal.repository.IdentificationRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.IdentificationUtils;
import com.mercadopago.android.px.internal.util.TextUtil;
import com.mercadopago.android.px.internal.viewmodel.PayerInformationStateModel;
import com.mercadopago.android.px.model.IdentificationType;
import com.mercadopago.android.px.model.Payer;
import com.mercadopago.android.px.model.PaymentMethod;
import com.mercadopago.android.px.model.exceptions.InvalidFieldException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.tracking.internal.views.CPFViewTracker;
import com.mercadopago.android.px.tracking.internal.views.LastNameViewTracker;
import com.mercadopago.android.px.tracking.internal.views.NameViewTracker;
import java.util.List;

/* default */ class PayerInformationPresenter extends BasePresenter<PayerInformation.View>
    implements PayerInformation.Actions {

    @NonNull /* default */ final PayerInformationStateModel state;
    @NonNull
    private final PaymentSettingRepository paymentSettings;

    @NonNull
    private final IdentificationRepository identificationRepository;

    @Nullable
    private final PaymentMethod paymentMethodInformation;

    private FailureRecovery mFailureRecovery;

    private static final int DEFAULT_IDENTIFICATION_NUMBER_LENGTH = 12;

    /* default */ PayerInformationPresenter(@NonNull final PayerInformationStateModel state,
        @NonNull final PaymentSettingRepository paymentSettings,
        @NonNull final IdentificationRepository identificationRepository,
        @Nullable final PaymentMethod paymentMethod) {
        this.state = state;
        this.paymentSettings = paymentSettings;
        this.identificationRepository = identificationRepository;
        this.paymentMethodInformation = paymentMethod;
    }

    @Override
    public void attachView(final PayerInformation.View view) {
        super.attachView(view);

        if (state.hasIdentificationTypes()) {
            view.hideProgressBar();
        } else {
            view.showProgressBar();
            getIdentificationTypesAsync();
        }

        if (state.hasFilledInfo()) {
            view.initializeIdentificationTypes(state.getIdentificationTypeList(), state.getIdentificationType());
            view.setName(state.getIdentificationName());
            view.setLastName(state.getIdentificationLastName());
            view.setNumber(state.getIdentificationNumber());
            view.identificationDraw();
        }
    }

    private void getIdentificationTypesAsync() {
        identificationRepository.getIdentificationTypes().enqueue(
            new TaggedCallback<List<IdentificationType>>(ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES) {
                @Override
                public void onSuccess(final List<IdentificationType> identificationTypes) {
                    resolveIdentificationTypes(identificationTypes);
                    getView().hideProgressBar();
                    getView().requestIdentificationNumberFocus();
                }

                @Override
                public void onFailure(final MercadoPagoError error) {
                    if (isViewAttached()) {
                        getView().showError(error, ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES);
                        setFailureRecovery(new FailureRecovery() {
                            @Override
                            public void recover() {
                                getView().showProgressBar();
                                getIdentificationTypesAsync();
                            }
                        });
                    }
                }
            });
    }

    private void resolveIdentificationTypes(final List<IdentificationType> identificationTypes) {
        state.setIdentificationTypes(identificationTypes);
        if (identificationTypes.isEmpty()) {
            getView().showMissingIdentificationTypesError();
        } else {
            getView().initializeIdentificationTypes(identificationTypes, state.getIdentificationType());
        }
    }

    public void saveIdentificationNumber(final String identificationNumber) {
        state.setIdentificationNumber(identificationNumber);
    }

    public void saveIdentificationName(final String identificationName) {
        state.setIdentificationName(identificationName);
    }

    public void saveIdentificationLastName(final String identificationLastName) {
        state.setIdentificationLastName(identificationLastName);
    }

    public void saveIdentificationBusinessName(final String identificationBusinessName) {
        state.saveIdentificationBusinessName(identificationBusinessName);
    }

    public int getIdentificationNumberMaxLength() {
        int maxLength = DEFAULT_IDENTIFICATION_NUMBER_LENGTH;

        if (state.getIdentificationType() != null) {
            maxLength = state.getIdentificationType().getMaxLength();
        }
        return maxLength;
    }

    public void saveIdentificationType(final IdentificationType identificationType) {
        state.setIdentificationType(identificationType);
        if (identificationType != null) {
            state.getIdentification().setType(identificationType.getId());
            getView().setIdentificationNumberRestrictions(identificationType.getType());
        }
    }

    public void createPayer() {
        //Get current payer
        final CheckoutPreference checkoutPreference = paymentSettings.getCheckoutPreference();
        final Payer payer = checkoutPreference.getPayer();
        // add collected information.

        //Business name is first name in v1/payments
        if (IdentificationUtils.isCnpj(state.getIdentificationType())) {
            payer.setFirstName(state.getIdentificationBusinessName());
            payer.setLastName(TextUtil.EMPTY);
        } else {
            payer.setFirstName(state.getIdentificationName());
            payer.setLastName(state.getIdentificationLastName());
        }
        payer.setIdentification(state.getIdentification());
        // reconfigure
        paymentSettings.configure(checkoutPreference);
    }

    public FailureRecovery getFailureRecovery() {
        return mFailureRecovery;
    }

    public void setFailureRecovery(final FailureRecovery failureRecovery) {
        mFailureRecovery = failureRecovery;
    }

    public void recoverFromFailure() {
        if (mFailureRecovery != null) {
            mFailureRecovery.recover();
        }
    }

    private void resolveInvalidFieldException(final InvalidFieldException e) {
        switch (e.getErrorCode()) {
        case InvalidFieldException.INVALID_IDENTIFICATION_LENGHT:
            getView().showInvalidLengthIdentificationNumberErrorView();
            getView().showErrorIdentificationNumber();
            break;
        default:
            getView().showInvalidIdentificationNumberErrorView();
            getView().showErrorIdentificationNumber();
            break;
        }
    }

    @Override
    public void validateName() {
        if (TextUtil.isNotEmpty(state.getIdentificationName())) {
            getView().clearErrorView();
            getView().clearErrorName();
            getView().showIdentificationLastNameFocus();
        } else {
            getView().showInvalidIdentificationNameErrorView();
            getView().showErrorName();
        }
    }

    @Override
    public void validateLastName() {
        if (TextUtil.isNotEmpty(state.getIdentificationLastName())) {
            getView().clearErrorView();
            getView().clearErrorLastName();
            getView().showCardFlowEnd();
        } else {
            getView().showInvalidIdentificationLastNameErrorView();
            getView().showErrorLastName();
        }
    }

    @Override
    public void validateBusinessName() {
        if (TextUtil.isNotEmpty(state.getIdentificationBusinessName())) {
            getView().clearErrorView();
            getView().clearErrorBusinessName();
            getView().showCardFlowEnd();
        } else {
            getView().showInvalidIdentificationBusinessNameErrorView();
            getView().showErrorBusinessName();
        }
    }

    @Override
    public void validateIdentification() {
        try {
            IdentificationUtils.validateTicketIdentification(state.getIdentification(), state.getIdentificationType());
            getView().clearErrorView();
            getView().clearErrorIdentificationNumber();
            showIdentificationNumberNextScreen();
        } catch (InvalidFieldException e) {
            resolveInvalidFieldException(e);
        }
    }

    private void showIdentificationNumberNextScreen() {
        if (IdentificationUtils.isCnpj(state.getIdentificationType())) {
            getView().showIdentificationBusinessNameFocus();
        } else {
            getView().showIdentificationNameFocus();
        }
    }

    @Override
    public void trackIdentificationNumberView() {
        final CPFViewTracker cpfViewTracker = new CPFViewTracker(paymentMethodInformation);
        setCurrentViewTracker(cpfViewTracker);
    }

    @Override
    public void trackIdentificationNameView() {
        final NameViewTracker nameViewTracker = new NameViewTracker(paymentMethodInformation);
        setCurrentViewTracker(nameViewTracker);
    }

    @Override
    public void trackIdentificationLastNameView() {
        final LastNameViewTracker lastNameViewTracker = new LastNameViewTracker(paymentMethodInformation);
        setCurrentViewTracker(lastNameViewTracker);
    }

    @Override
    public void trackAbort() {
        tracker.trackAbort();
    }

    @Override
    public void trackBack() {
        tracker.trackBack();
    }

    @NonNull
    @Override
    public PayerInformationStateModel getState() {
        return state;
    }

    @Override
    public void focus(final String currentFocusType) {
        state.setFocus(currentFocusType);
    }

    @Override
    public void configureIdentificationTypeFlow(@NonNull final IdentificationType identificationType) {
        if (IdentificationUtils.isCnpj(identificationType)) {
            getView().configureCnpjFlow();
        } else {
            getView().configureCpfFlow();
        }
    }
}

package com.nocountry.conversionflow.conversionflow_api.application.port;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;

public interface ConversionDispatchHandler {

    Provider provider();

    void dispatch(String payload);
}

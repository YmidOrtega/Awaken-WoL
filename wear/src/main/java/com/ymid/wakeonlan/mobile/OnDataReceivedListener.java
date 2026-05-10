package com.ymid.wakeonlan.mobile;

import java.util.List;

import com.ymid.wakeonlan.models.DeviceDto;

public interface OnDataReceivedListener {

    void onDataReceived(List<DeviceDto> devices);

    void onError(Exception e);
}

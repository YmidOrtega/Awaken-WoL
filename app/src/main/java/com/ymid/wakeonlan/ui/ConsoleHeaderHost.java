package com.ymid.wakeonlan.ui;

import androidx.annotation.Nullable;

/** Lets a screen fill the console header shown above it. Both values reset on every navigation. */
public interface ConsoleHeaderHost {

    void setHeaderSubtitle(@Nullable CharSequence subtitle);

    /** Shows a figure such as "2/3" with an "online" caption; null hides it. */
    void setHeaderCounter(@Nullable CharSequence counter);
}

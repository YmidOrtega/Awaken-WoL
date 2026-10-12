package com.ymid.wakeonlan.ui;

import androidx.annotation.Nullable;

/** Lets a screen fill the console header shown above it. Both values reset on every navigation. */
public interface ConsoleHeaderHost {

    void setHeaderSubtitle(@Nullable CharSequence subtitle);

    /** Shows a figure such as "2/3" above a short caption; a null counter hides both. */
    void setHeaderCounter(@Nullable CharSequence counter, @Nullable CharSequence caption);
}

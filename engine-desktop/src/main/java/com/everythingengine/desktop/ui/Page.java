package com.everythingengine.desktop.ui;

import javafx.scene.Node;

public interface Page {
    Node node();

    default void onShown() {
    }
}

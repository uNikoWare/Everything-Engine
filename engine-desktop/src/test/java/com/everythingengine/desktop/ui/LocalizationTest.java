package com.everythingengine.desktop.ui;

import com.everythingengine.desktop.settings.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationTest {
    @Test
    void translatesEnglishUiLabelsToGermanAndKeepsEnglishDefault() {
        assertEquals("Einstellungen", Localization.text("Settings", Language.GERMAN));
        assertEquals("Settings", Localization.text("Settings", Language.ENGLISH));
        assertEquals("Generierung", Localization.text("Generation", Language.GERMAN));
        assertEquals("Namensgenerierung", Localization.text("Name generation", Language.GERMAN));
    }
}

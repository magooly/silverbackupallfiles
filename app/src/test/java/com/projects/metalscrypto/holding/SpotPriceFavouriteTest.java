package com.projects.metalscrypto.holding;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SpotPriceFavouriteTest {
    @Test
    public void selectionRoundTripPreservesOrderAndAssetIdentity() {
        List<SpotPriceFavourite> selected = SpotPriceFavourite.defaults();
        SpotPriceFavourite.add(selected, SpotPriceFavourite.stock("tls"));
        SpotPriceFavourite.add(selected, SpotPriceFavourite.crypto("Bitcoin", "bitcoin"));
        Collections.swap(selected, 0, 3);
        selected.remove(1);
        List<SpotPriceFavourite> restored = SpotPriceFavourite.restore(SpotPriceFavourite.serialize(selected));
        assertEquals(3, restored.size());
        for (int index = 0; index < selected.size(); index++) {
            assertEquals(selected.get(index).id(), restored.get(index).id());
            assertEquals(selected.get(index).unit, restored.get(index).unit);
            assertEquals(selected.get(index).currency, restored.get(index).currency);
        }
    }

    @Test
    public void emptySelectionRemainsEmptyAfterRestart() {
        assertTrue(SpotPriceFavourite.restore("[]").isEmpty());
    }

    @Test
    public void damagedPreferencesRestoreSafeDefaults() {
        assertEquals("metal:Gold", SpotPriceFavourite.restore("broken-json").get(0).id());
    }

    @Test
    public void defaultsAreGoldAndSilverInOrder() {
        List<SpotPriceFavourite> favourites = SpotPriceFavourite.defaults();
        assertEquals("metal:Gold", favourites.get(0).id());
        assertEquals("metal:Silver", favourites.get(1).id());
    }

    @Test
    public void stocksAreCanonicalAndRejectInvalidSymbols() {
        assertEquals("stock:TLS", SpotPriceFavourite.stock("Tls.ax").id());
        assertNull(SpotPriceFavourite.stock("not a ticker"));
    }

    @Test
    public void duplicateAssetsCannotOccupyTwoSlots() {
        List<SpotPriceFavourite> favourites = new ArrayList<>();
        assertTrue(SpotPriceFavourite.add(favourites, SpotPriceFavourite.stock("TLS")));
        assertFalse(SpotPriceFavourite.add(favourites, SpotPriceFavourite.stock("tls")));
    }

    @Test
    public void limitIsSixAndSelectionOrderIsPreserved() {
        List<SpotPriceFavourite> favourites = SpotPriceFavourite.defaults();
        for (String ticker : new String[] { "TLS", "BHP", "ANZ", "CBA" }) {
            assertTrue(SpotPriceFavourite.add(favourites, SpotPriceFavourite.stock(ticker)));
        }
        assertFalse(SpotPriceFavourite.add(favourites, SpotPriceFavourite.stock("NAB")));
        assertEquals("stock:CBA", favourites.get(5).id());
    }

    @Test
    public void unitAndCurrencyAreExplicitAndOtherItemsAreNotMetals() {
        assertEquals("crypto:bitcoin", SpotPriceFavourite.crypto("Bitcoin", "BITCOIN").id());
        assertNull(SpotPriceFavourite.metal("Other"));
        assertEquals("USD", SpotPriceFavourite.metal("Gold").currency);
        assertEquals("oz", SpotPriceFavourite.metal("Gold").unit);
        assertEquals("AUD", SpotPriceFavourite.stock("BHP").currency);
        assertEquals("coin", SpotPriceFavourite.crypto("Bitcoin", "bitcoin").unit);
    }
}
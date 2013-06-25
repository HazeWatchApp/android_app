package me.ebernie.mapi.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {
	
	/*
	 * Document doc = Jsoup.connect(DOE_URL).userAgent(UA_STRING)
                        .referrer(REFERRER).get();
                LOG.info("Document baseURI: " + doc.baseUri());
                for (Element table : doc.select("table[class=table1]")) {
                    for (Element row : table.select("tr:gt(2)")) {
                        Elements tds = row.select("td:not([rowspan])");
                        AirPolutionIndex index = new AirPolutionIndex(tds.get(0).text(), tds.get(1).text(), tds.get(2).text(), tds.get(3).text(), tds.get(4).text());
                        indices.add(index);
                        //use area as keys for memcache
                        keys.add(index.getArea());
                        syncCache.put(index.getArea(), index, Expiration.byDeltaMillis(expirationInMiliseconds));
                    }
                }
	 */

	/**
	 * An array of sample (dummy) items.
	 */
	public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static Map<String, DummyItem> ITEM_MAP = new HashMap<String, DummyItem>();

	static {
		// Add 3 sample items.
		addItem(new DummyItem("1", "Item 1"));
		addItem(new DummyItem("2", "Item 2"));
		addItem(new DummyItem("3", "Item 3"));
	}

	private static void addItem(DummyItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}

	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public String id;
		public String content;

		public DummyItem(String id, String content) {
			this.id = id;
			this.content = content;
		}

		@Override
		public String toString() {
			return content;
		}
	}
}

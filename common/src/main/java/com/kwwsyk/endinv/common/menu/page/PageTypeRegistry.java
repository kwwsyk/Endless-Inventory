package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;

import java.util.*;

import static com.kwwsyk.endinv.common.menu.page.PageType.*;

public class PageTypeRegistry {

    public static final int ORDER_INTERVAL = 0x100;

    private static final LinkedHashMap<String, PageType> PAGE_TYPES = new LinkedHashMap<>();
    private static final List<String> INDEX_LIST = new ArrayList<>();
    private static final List<Integer> ORDER = new ArrayList<>();

    private static List<PageType> cache = null;

    static {
        register(ALL_ITEMS,0);
        register(BLOCK_ITEMS,0x100);
        register(WEAPONS,0x300);
        register(TOOLS,0x200);
        register(EQUIPMENTS);
        register(CONSUMABLE);
        register(ENCHANTED_BOOKS);
        register(BOOKMARK);
    }

    public static void register(PageType type) {
        String id = type.registerName;
        if (PAGE_TYPES.containsKey(id)) throw new IllegalArgumentException("Duplicate page id: " + id);
        PAGE_TYPES.put(id, type);
        INDEX_LIST.add(id);
        ORDER.add(getLastPageOrder() + ORDER_INTERVAL);
        cache = null;//inlined
    }

    public static void register(PageType type, int order) {
        String id = type.registerName;
        if (PAGE_TYPES.containsKey(id)) throw new IllegalArgumentException("Duplicate page id: " + id);
        PAGE_TYPES.put(id, type);
        INDEX_LIST.add(id);
        ORDER.add(order);
        cache = null;//inlined
    }

    public static List<PageType> getDisplayPages(){
        return cache!=null? cache :
                (cache = getSortedList().stream()
                        .filter(str-> !ClientModInfo.getClientConfig().hidingPageIds().contains(str))
                        .map(PAGE_TYPES::get)
                        .toList()
                );
    }

    public static int getLastPageOrder(){
        return ORDER.stream().filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
    }

    public static List<String> getSortedList(){
        List<Pair<String,Integer>> ret = new ArrayList<>(size());
        for(int i=0; i<size(); ++i){
            ret.add(new ObjectIntImmutablePair<>(INDEX_LIST.get(i),ORDER.get(i)));
        }
        ret.sort(Comparator.comparingInt(Pair::right));
        return ret.stream().map(Pair::left).toList();
    }

    public static PageType byId(String id) {
        return PAGE_TYPES.get(id);
    }

    public static PageType byIndex(int index) {
        return PAGE_TYPES.get(INDEX_LIST.get(index));
    }

    public static int size(){
        return INDEX_LIST.size();
    }

    public static int getIndexOf(String id) {
        return INDEX_LIST.indexOf(id);
    }

    public static Collection<PageType> values() {
        return PAGE_TYPES.values();
    }

    public static void setChanged(){
        cache = null;
    }

    public static List<String> getIdList(){
        return new ArrayList<>(INDEX_LIST);
    }
}

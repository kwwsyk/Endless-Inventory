package com.kwwsyk.endinv.common.options;

/**
 * Controls how server Endless Inventory transfer contents to client,
 * all for all items and part for only send to be displayed items in case the item size was too big.
 */
public enum ContentTransferMode {

    ALL,
    PART;
}

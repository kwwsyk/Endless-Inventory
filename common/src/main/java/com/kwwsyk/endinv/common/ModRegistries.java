package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.item.ScreenDebugger;
import com.kwwsyk.endinv.common.item.TestEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.SyncedConfig;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;

import java.util.UUID;
import java.util.function.Supplier;

public final class ModRegistries {

    private ModRegistries(){}

    private static Registry<PageType> PAGE_TYPE_REG;


    static void setPageTypeReg(Registry<PageType> reg){
        PAGE_TYPE_REG = reg;
    }

    public static Registry<PageType> getPageTypeReg() {
        return PAGE_TYPE_REG;
    }

    public static class Items{

        static Supplier<TestEndInv> testEndInv;
        static Supplier<ScreenDebugger> screenDebugger;

        public static TestEndInv getTestEndInv(){
            return testEndInv.get();
        }

        public static ScreenDebugger getScreenDebugger(){
            return screenDebugger.get();
        }
    }

    public static class Menus{

        static Supplier<MenuType<EndlessInventoryMenu>> endinvMenuType;

        public static MenuType<EndlessInventoryMenu> getEndInvMenuType(){
            return endinvMenuType.get();
        }
    }

    public static class NbtAttachments{

        static NbtAttachment<UUID> endInvUUID;
        static NbtAttachment<SyncedConfig> syncedConfig;

        public static NbtAttachment<UUID> getEndInvUUID(){
            return endInvUUID;
        }

        public static NbtAttachment<SyncedConfig> getSyncedConfig(){
            return syncedConfig;
        }
    }
}

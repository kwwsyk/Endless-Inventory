package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.item.ScreenDebugger;
import com.kwwsyk.endinv.common.item.TestEndInv;
import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ModRegistries {

    private ModRegistries(){}

    public static class RegistryImpl<T> implements Registry<T>{

        private ResourceKey<T> key;

        @Override
        public ResourceKey<? extends Registry> key() {
            return key;
        }

        @Override
        public @Nullable ResourceLocation getKey(Object o) {
            return key.location();
        }

        @Override
        public Optional<ResourceKey> getResourceKey(Object o) {
            return Optional.empty();
        }

        @Override
        public int getId(@Nullable Object o) {
            return 0;
        }

        @Override
        public @Nullable Object byId(int i) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public @Nullable Object get(@Nullable ResourceKey resourceKey) {
            return null;
        }

        @Override
        public @Nullable Object get(@Nullable ResourceLocation resourceLocation) {
            return null;
        }

        @Override
        public Optional<RegistrationInfo> registrationInfo(ResourceKey resourceKey) {
            return Optional.empty();
        }

        @Override
        public Lifecycle registryLifecycle() {
            return null;
        }

        @Override
        public Optional<Holder.Reference> getAny() {
            return Optional.empty();
        }

        @Override
        public Set<ResourceLocation> keySet() {
            return Set.of();
        }

        @Override
        public Set<Map.Entry> entrySet() {
            return Set.of();
        }

        @Override
        public Set<ResourceKey> registryKeySet() {
            return Set.of();
        }

        @Override
        public Optional<Holder.Reference> getRandom(RandomSource randomSource) {
            return Optional.empty();
        }

        @Override
        public boolean containsKey(ResourceLocation resourceLocation) {
            return false;
        }

        @Override
        public boolean containsKey(ResourceKey resourceKey) {
            return false;
        }

        @Override
        public Registry freeze() {
            return null;
        }

        @Override
        public Holder.Reference createIntrusiveHolder(Object o) {
            return null;
        }

        @Override
        public Optional<Holder.Reference> getHolder(int i) {
            return Optional.empty();
        }

        @Override
        public Optional<Holder.Reference> getHolder(ResourceLocation resourceLocation) {
            return Optional.empty();
        }

        @Override
        public Optional<Holder.Reference> getHolder(ResourceKey resourceKey) {
            return Optional.empty();
        }

        @Override
        public Holder wrapAsHolder(Object o) {
            return null;
        }

        @Override
        public Stream<Holder.Reference> holders() {
            return Stream.empty();
        }

        @Override
        public Optional<HolderSet.Named> getTag(TagKey tagKey) {
            return Optional.empty();
        }

        @Override
        public HolderSet.Named getOrCreateTag(TagKey tagKey) {
            return null;
        }

        @Override
        public Stream<Pair<TagKey, HolderSet.Named>> getTags() {
            return Stream.empty();
        }

        @Override
        public Stream<TagKey> getTagNames() {
            return Stream.empty();
        }

        @Override
        public void resetTags() {

        }

        @Override
        public HolderOwner holderOwner() {
            return null;
        }

        @Override
        public HolderLookup.RegistryLookup asLookup() {
            return null;
        }

        @Override
        public void bindTags(Map map) {

        }

        @Override
        public @NotNull Iterator iterator() {
            return null;
        }
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
}

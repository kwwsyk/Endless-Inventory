package com.kwwsyk.endinv.common.options.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed abstract class ConfigEntryImpl<T> implements AutoSavable<T>, IConfigValue<T>
        permits ComplexConfigEntryImpl, ConfigEntryImpl.BooleanEntry, ConfigEntryImpl.EnumEntry, ConfigEntryImpl.ListEntry, ConfigEntryImpl.RangedEntry, ConfigEntryImpl.StringEntry
{

    protected static final Logger LOGGER = LogUtils.getLogger();
    public boolean checkInitializationStrict = false;

    protected final String key;
    protected final Supplier<String[]> comments;
    protected final T defaultValue;

    protected Supplier<T> getter;
    protected Consumer<? super T> setter;
    protected Runnable saver;

    protected boolean isInitialized = false;

    public ConfigEntryImpl(String key, String[] comments, T defaultValue, Supplier<T> getter, Consumer<T> setter){
        this.key = key;
        this.comments = new LocaleComments(comments);
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
        isInitialized = true;
    }

    public ConfigEntryImpl(String key, String[] comments, T defaultValue){
        this.key = key;
        this.comments = new LocaleComments(comments);
        this.defaultValue = defaultValue;
    }

    public void initialize(Supplier<T> getter, Consumer<? super T> setter){
        if(isInitialized && checkInitializationStrict){
            LOGGER.warn("ConfigEntry {} is already initialized!", key);
        }
        this.getter = getter;
        this.setter = setter;
        isInitialized = true;
    }

    public void setSaver(Runnable saver){
        this.saver = saver;
    }

    @Override
    public void save(){
        if(saver!=null && isInitialized){
            saver.run();
        }else if(checkInitializationStrict){
            throw new IllegalStateException("Try to save config but it is not initialized.");
        }
    }

    public String key() {
        return key;
    }

    public String[] comments() {
        return comments.get();
    }

    public T defaultValue() {
        return defaultValue;
    }

    @Override
    public T get() {
        if(!isInitialized && checkInitializationStrict){
            throw new IllegalStateException("ConfigEntry "+key+" is not initialized yet!");
        }
        return getter.get();
    }

    @Override
    public void set(T t) {
        if(!isInitialized && checkInitializationStrict){
            throw new IllegalStateException("ConfigEntry "+key+" is not initialized yet!");
        }
        setter.accept(t);
        save();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public List<String> print(){
        List<String> list = new ArrayList<>();
        list.add(key() + ": " + (isInitialized() ? "Current: " + get() : ""));
        for(String cmt : comments()) list.add("   #" + cmt);
        list.add("    default: " + defaultValue);
        return list;
    }

    @Override
    public String toString() {
        return isInitialized() ? key() + "=" + get() : key() + ", default value: " + defaultValue();
    }

    public static final class BooleanEntry extends ConfigEntryImpl<Boolean> implements IConfigValue<Boolean>, AutoSavable<Boolean> {
        public BooleanEntry(String key, String[] comments, boolean defaultValue){
            super(key,comments,defaultValue);
        }
    }

    public static abstract sealed class RangedEntry<N extends Comparable<? super N>> extends ConfigEntryImpl<N> implements IConfigValue<N>, AutoSavable<N> {
        private final N min;
        private final N max;

        public RangedEntry(String key, String[] comments, N defaultValue, N min, N max){
            super(key,comments,defaultValue);
            this.min = min;
            this.max = max;
        }

        public N getMin(){
            return min;
        }
        public N getMax(){
            return max;
        }
    }

    public static final class IntEntry extends RangedEntry<Integer>{
        public IntEntry(String key, String[] comments, int defaultValue){
            this(key,comments,defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        public IntEntry(String key, String[] comments, int defaultValue, int min, int max){
            super(key,comments,defaultValue,min,max);
        }
    }

    public static final class StringEntry extends ConfigEntryImpl<String> implements IConfigValue<String>, AutoSavable<String> {
        public StringEntry(String key, String[] comments, String defaultValue){
            super(key,comments,defaultValue);
        }
    }

    public static final class DoubleEntry extends RangedEntry<Double>{
        public DoubleEntry(String key, String[] comments, double defaultValue){
            this(key,comments,defaultValue,Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public DoubleEntry(String key, String[] comments, double defaultValue, double min, double max){
            super(key,comments,defaultValue,min,max);
        }
    }

    public static final class EnumEntry<E extends Enum<E>> extends ConfigEntryImpl<E> implements IConfigValue<E>, AutoSavable<E> {
        public EnumEntry(String key, String[] comments, E defaultValue){
            super(key,comments,defaultValue);
        }

        public void initialize(Supplier<E> getter, Consumer<? super E> setter) {
            super.initialize(getter, setter);
        }

        @SuppressWarnings("unchecked")
        public void initialize1(Supplier<? extends Enum<?>> getter, Consumer setter){
            initialize((Supplier<E>) getter, setter);
        }
    }
    public static final class ListEntry<E> extends ConfigEntryImpl<List<E>> implements IConfigValue<List<E>>, AutoSavable<List<E>> {
        private Supplier<? extends E> newValSupplier = null;
        private Predicate<Object> newValPredicate = o -> true;
        private int minLen = 0,maxLen = Integer.MAX_VALUE;

        public ListEntry(String key, String[] comments, List<E> defaultValue){
            super(key,comments,defaultValue);
        }

        public ListEntry(String key, String[] comments, List<E> defaultValue,@Nullable Supplier<E> newValSupplier,Predicate<Object> newValPredicate){
            super(key,comments,defaultValue);
            this.newValSupplier = newValSupplier;
            this.newValPredicate = newValPredicate;
        }
        /**Satisfy forge*/
        @Nullable
        public Supplier<? extends E> getNewValSupplier() {
            return newValSupplier;
        }

        public Predicate<Object> getNewValPredicate() {
            return newValPredicate;
        }

        public int getMinLen() {
            return minLen;
        }

        public int getMaxLen() {
            return maxLen;
        }

        public ListEntry<E> setRange(int minLen, int maxLen) {
            if(minLen > maxLen || minLen < 0){
                throw new IllegalArgumentException("minLen must be less than maxLen and greater than 0");
            }
            this.minLen = minLen;
            this.maxLen = maxLen;
            return this;
        }
    }
    public static final class FloatEntry extends RangedEntry<Float>{
        public FloatEntry(String key, String[] comments, float defaultValue){
            this(key,comments,defaultValue,Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        public FloatEntry(String key, String[] comments, float defaultValue, float min, float max){
            super(key,comments,defaultValue,min,max);
        }
    }
    public static final class LongEntry extends RangedEntry<Long>{
        public LongEntry(String key, String[] comments, long defaultValue){
            this(key,comments,defaultValue,Long.MIN_VALUE,Long.MAX_VALUE);
        }

        public LongEntry(String key, String[] comments, long defaultValue, long min, long max){
            super(key,comments,defaultValue,min,max);
        }
    }
}

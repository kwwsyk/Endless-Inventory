package com.kwwsyk.endinv.common.options.config;

import java.util.ArrayList;
import java.util.List;

/**Configuration entry of grouped configurations.
 *</br>
 * Supports recursive installation via (neo)forge's night config api by iterate {@link #fields()}
 *
 * <p>
 *     For usage, access config fields by such format <br>
 *     {@code Configs.A_GROUP.AConfigValue.get()}
 * </p>
 *
 *
 * @implNote define public final {@link ConfigEntryImpl} fields and add them to {@link #fields()}
 * <p>
 *     define fields:<p>
 *     {@code public final ConfigEntryImpl AField = ...}<br>
 *     {@code fields(){ return []{AField,BField...} }}
 *     <br>
 *     for getter and setter
 *     overwrite if record representation is existing
 *     and use {@link #freezeFieldSaver()} and {@link #unfreezeFieldSaver()} in setter to reduce useless saver cost.
 * </p>
 *
 * @param <C> a record that presents the fields or Void.
 */
public abstract non-sealed class ComplexConfigEntryImpl<C> extends ConfigEntryImpl<C>{

    private boolean frozen;
    protected class LazySaver implements Runnable{
        @Override
        public void run() {
            if(frozen) return;
            save();
        }
    }

    protected void freezeFieldSaver(){
        frozen = true;
    }

    protected void unfreezeFieldSaver(){
        frozen = false;
    }

    public ComplexConfigEntryImpl(String key, String[] comments, C defaultValue) {
        super(key, comments, defaultValue);
    }

    public ComplexConfigEntryImpl(String key){
        super(key, new String[]{}, null);
    }

    public C defaultValue(){
        if(defaultValue == null){
            throw new UnsupportedOperationException();
        }
        return defaultValue;
    }

    /**
     * The getter of a complex config entry may be null.
     * Let it return {@link #fields()}'s returns.
     * <br>
     * If C is a record, let it return a new instance of C and invoke all fields' getters.
     *
     * @return the default value of the complex config entry.
     * @throws UnsupportedOperationException getter of {@link ComplexConfigEntryImpl} throws by default.
     * Better directly access the fields.
     */
    @Override
    public C get(){
        throw new UnsupportedOperationException();
    }

    /**
     * The setter of a complex config entry may be null.
     * Let it set every field of {@link #fields()}.
     * @param c to be used to set fields.
     */
    @Override
    public void set(C c){
        throw new UnsupportedOperationException();
    }

    public void setInitialized() {
        isInitialized = true;
    }

    public abstract ConfigEntryImpl<?>[] fields();

    @Override
    public void setSaver(Runnable saver){
        super.setSaver(saver);
        for(var field : fields()){
            field.setSaver(new LazySaver());
        }
    }

    public List<String> print(){
        List<String> list = new ArrayList<>();
        list.add(key() + ": ");
        for(String cmt : comments()) list.add("   #" + cmt);
        for(var field : fields()) list.add(field.toString());
        return list;
    }

    @Override
    public String toString(){
        return key() + (defaultValue != null ? ", default value: " + defaultValue() : " Complex Config");
    }
}

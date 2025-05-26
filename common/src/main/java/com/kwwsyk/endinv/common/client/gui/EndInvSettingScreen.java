package com.kwwsyk.endinv.common.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.kwwsyk.endinv.common.client.CachedSrcInv.INSTANCE;
import static com.kwwsyk.endinv.common.client.config.ClientConfig.CONFIG;

public class EndInvSettingScreen extends Screen {

    private static final ResourceLocation BLANK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final int CONFIG_ENTRY_Y_OFFSET = 17;
    private static final int CONFIG_ENTRY_X_OFFSET = 10;
    private static final int ENTRY_HEIGHT = 20,MAX_ENTRY_COUNT = 7;
    private static final int WIDGET_X_OFFSET = 165;
    private static final int WIDGET_X_SIZE = 60;
    private static final int WIDGET_Y_SIZE = 18;//y offset of entry: +1

    private final Screen back;
    private int leftPos,topPos;
    private final int imageWidth = 248;
    private final int imageHeight = 166;
    private int pageIndex = 0;
    private int entryOffset = 0;
    private double scrollOffset = 0;

    public final List<EntryBuilder> entries = new ArrayList<>();
    public final EntryBuilder[] renderingEntries = new EntryBuilder[7];
    @Nullable
    private EditBox typingEditBox;

    public EndInvSettingScreen(Screen lastScreen) {
        super(Component.translatable("title.endinv.settings"));
        this.back = lastScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        switchPage();
    }

    private void switchPage(){

        this.entries.clear();
        Arrays.fill(renderingEntries, null);
        this.clearWidgets();

        addPageSwitchButton();

        if (pageIndex == 1) {
            //entries.add(createAccessibilityConfig());
            addInfoEntry(Component.translatable("endinv.info.accessibility"),INSTANCE::getAccessibility);
            addInfoEntry(Component.translatable("endinv.info.owner_uuid"),INSTANCE::getOwnerUUID);
            addInfoEntry(Component.translatable("endinv.info.white_list_size"),()->"Size :"+INSTANCE.white_list.size());
        } else {
            addConfigEntry("endinv.setting.rows", CONFIG.ROWS);
            addConfigEntry("endinv.setting.columns", CONFIG.COLUMNS);
            addConfigEntry("endinv.setting.auto_suit", CONFIG.AUTO_SUIT_COLUMN);
            addConfigEntry("endinv.setting.attaching", CONFIG.ATTACHING);
            addConfigEntry("endinv.setting.texture", CONFIG.TEXTURE);
            addConfigEntry("endinv.setting.max_page_bar", CONFIG.MAX_PAGE_BARS);
            addConfigEntry(Component.literal("Screen debug"), CONFIG.ENABLE_DEBUG);
        }
        scrollTo();
    }

    private void scrollTo(){
        for(int i = 0; i<Math.min(MAX_ENTRY_COUNT,entries.size()); ++i){
            assert entryOffset<entries.size()-i;
            renderingEntries[i] = entries.get(i + entryOffset);
        }
        for (var entry : renderingEntries){
            if(entry==null) continue;
            entry.build();
            entry.syncConfig();
        }
    }

    private void pageSwitched(int index){
        this.pageIndex = index;
        switchPage();
    }

    private void addConfigEntry(String key, ModConfigSpec.ConfigValue<?> configValue){
        addConfigEntry(Component.translatable(key),configValue);
    }

    private void addConfigEntry(Component tip, ModConfigSpec.ConfigValue<?> configValue){
        entries.add(new ConfigEntry<>(entries.size(),tip,configValue));
    }

    private void addInfoEntry(Component tip, Supplier<Object> info){
        entries.add(new InfoEntry(entries.size(),tip,info));
    }

    private void addPageSwitchButton(){
        Button left = Button.builder(Component.literal("<"),btn-> pageSwitched(Mth.clamp(pageIndex-1,0,1)))
                .pos(leftPos-20,topPos)
                .size(20,20)
                .build();
        Button right = Button.builder(Component.literal(">"),btn-> pageSwitched(Mth.clamp(pageIndex+1,0,1)))
                .pos(leftPos+imageWidth+1,topPos)
                .size(20,20)
                .build();
        addRenderableWidget(left);
        addRenderableWidget(right);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        for(var entry : renderingEntries){
            if(entry!=null) {
                entry.render(guiGraphics, partialTick, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(BLANK_LOCATION,leftPos,topPos,0,0,imageWidth,imageHeight);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.back);
    }

    @Override
    public boolean charTyped(char code,int modifiers){
        if(super.charTyped(code,modifiers)){
            this.typingEditBox = this.getFocused() instanceof EditBox editBox ? editBox : null;

        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        boolean flag = this.getFocused() == null || !this.getFocused().isFocused() || this.getFocused() != typingEditBox;
        if(flag && this.typingEditBox !=null){
            for(var entry : entries){
                entry.getEditBox().ifPresent(editBox -> {
                    if(editBox== typingEditBox){
                        entry.applyChanges();
                        typingEditBox =null;
                    }
                });
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputConstants.KEY_RETURN && this.getFocused()!=null){
            this.getFocused().setFocused(false);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.getFocused()!=null || button==1){
            this.getFocused().setFocused(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(entries.size()>MAX_ENTRY_COUNT){
            if (scrollOffset < 1 && scrollY < 0) {
                scrollOffset = Mth.clamp(scrollOffset+scrollY,0.0,1.0);
            }
            if (scrollOffset > 0 && scrollY > 0) {
                scrollOffset = Mth.clamp(scrollOffset-scrollY,0.0,1.0);
            }
            entryOffset = (int) Math.floor(scrollOffset*(entries.size()-MAX_ENTRY_COUNT));
            scrollTo();
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /*private AttributeEntry<Accessibility> createAccessibilityConfig(){
        //to-do sendPacket
        return new AttributeEntry<>(entries.size(), Component.translatable("endinv.attr.accessibility"),
                INSTANCE::getAccessibility,
                INSTANCE::setAccessibility
        ) {
            @Override
            Accessibility parse(String s) {
                try{
                    return Accessibility.valueOf(s);
                } catch (Exception ex) {
                    return null;
                }
            }
        };
    }*/

    public interface EntryBuilder{

        void build();

        void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

        void syncConfig();

        void applyChanges();

        default Optional<EditBox> getEditBox(){
            return Optional.empty();
        }
    }

    public class InfoEntry implements EntryBuilder{

        private final Component tip;
        private final Supplier<Object> info;
        int widgetMidX,widgetY;

        public InfoEntry(int index, Component tip, Supplier<Object> info) {
            this.tip = tip;
            this.info = info;
            widgetMidX = leftPos+WIDGET_X_OFFSET+WIDGET_X_SIZE/2 - 40;
            widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+index*ENTRY_HEIGHT+1;
        }

        @Override
        public void build() {
        }

        @Override
        public void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            Font font = EndInvSettingScreen.this.font;
            guiGraphics.drawString(font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
            Component v = Component.literal(info.get()!=null? info.get().toString():"null");
            int infoLength = font.width(v.getVisualOrderText());
            guiGraphics.drawString(font,v,widgetMidX-infoLength/2,widgetY,0xFF00FFFF);
        }

        @Override
        public void syncConfig() {
        }

        @Override
        public void applyChanges() {
        }
    }

    public abstract class AttributeEntry<T> implements EntryBuilder{

        public final int index;
        private final Component tip;
        private final Supplier<T> attributeGetter;
        private final Consumer<T> attributeSetter;
        private EditBox editBox;
        int widgetX,widgetY;

        public AttributeEntry(int index, Component tip, Supplier<T> attributeGetter, Consumer<T> attributeSetter) {
            this.index = index;
            this.tip = tip;
            this.attributeGetter = attributeGetter;
            this.attributeSetter = attributeSetter;
            widgetX = leftPos+WIDGET_X_OFFSET;
            widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+index*ENTRY_HEIGHT+1;
        }

        @Override
        public void build() {
            EditBox editBox = new EditBox(EndInvSettingScreen.this.font,widgetX,widgetY,WIDGET_X_SIZE,WIDGET_Y_SIZE,Component.empty());
            EndInvSettingScreen.this.addRenderableWidget(editBox);
            this.editBox = editBox;
        }

        @Override
        public void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
            guiGraphics.drawString(EndInvSettingScreen.this.font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
        }

        @Override
        public void syncConfig() {
            editBox.setValue(attributeGetter.get().toString());
        }

        @Override
        public void applyChanges() {
            T t = parse(editBox.getValue());
            if(t==null){
                editBox.setValue(attributeGetter.get().toString());
            }else {
                attributeSetter.accept(t);
            }
        }

        abstract T parse(String s);

        public Optional<EditBox> getEditBox(){
            return Optional.of(editBox);
        }
    }

    public class StringAttributeEntry extends AttributeEntry<String>{

        public StringAttributeEntry(int index, Component tip, Supplier<String> attributeGetter, Consumer<String> attributeSetter) {
            super(index, tip, attributeGetter, attributeSetter);
        }

        @Override
        String parse(String s) {
            return s;
        }
    }

    public class IntAttributeEntry extends AttributeEntry<Integer>{

        public IntAttributeEntry(int index, Component tip, Supplier<Integer> attributeGetter, Consumer<Integer> attributeSetter) {
            super(index, tip, attributeGetter, attributeSetter);
        }

        @Override
        Integer parse(String s) {
            try{
                return Integer.parseInt(s);
            }catch (NumberFormatException e){
                return null;
            }
        }
    }

    public class EnumAttributeEntry<E extends Enum<?>> extends AttributeEntry<Enum<?>>{

        E e;

        public EnumAttributeEntry(int index, Component tip, Supplier<Enum<?>> attributeGetter, Consumer<Enum<?>> attributeSetter) {
            super(index, tip, attributeGetter, attributeSetter);
        }

        @Override
        @SuppressWarnings("unchecked")
        Enum<?> parse(String s) {
            try{
                return Enum.valueOf(e.getClass(), s);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public class ConfigEntry<T> implements EntryBuilder{

        public final int index;
        private final Component tip;
        private final ModConfigSpec.ConfigValue<T> configValue;
        private final T initialValue;
        private AbstractWidget configWidget;
        int widgetX,widgetY;


        public ConfigEntry(int index, Component tip, ModConfigSpec.ConfigValue<T> configValue){
            this.index = index;
            this.tip = tip;
            this.configValue = configValue;
            this.initialValue = configValue.get();
            widgetX = leftPos+WIDGET_X_OFFSET;
            widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+index*ENTRY_HEIGHT+1;
        }

        @SuppressWarnings("unchecked")
        public void build(){
            switch (configValue){
                case ModConfigSpec.BooleanValue booleanValue -> {
                    var button = CycleButton.onOffBuilder((Boolean) initialValue)
                            .displayOnlyValue()
                            .create(widgetX,widgetY,WIDGET_X_SIZE,WIDGET_Y_SIZE,Component.empty(), (btn,value)-> booleanValue.set(value));
                    this.configWidget = button;
                    EndInvSettingScreen.this.addRenderableWidget(button);
                }
                case ModConfigSpec.EnumValue<?> enumValue -> {
                    assert initialValue instanceof Enum<?>;
                    var button = new CycleButton.Builder<Enum<?>>(e-> Component.translatable("endinv.setting.entry."+e.name()))
                            .withValues((Enum<?>[]) initialValue.getClass().getEnumConstants())
                            .withInitialValue((Enum<?>) initialValue)
                            .displayOnlyValue()
                            .create(widgetX,widgetY,WIDGET_X_SIZE,WIDGET_Y_SIZE,Component.empty(),(btn,value)->((ModConfigSpec.EnumValue)enumValue).set(value));
                    this.configWidget = button;
                    EndInvSettingScreen.this.addRenderableWidget(button);
                }
                case ModConfigSpec.IntValue ignored -> {
                    EditBox editBox = new EditBox(EndInvSettingScreen.this.font,widgetX,widgetY,WIDGET_X_SIZE,WIDGET_Y_SIZE,tip);
                    this.configWidget = editBox;
                    EndInvSettingScreen.this.addRenderableWidget(editBox);
                }
                default -> {
                    EndInvSettingScreen self = EndInvSettingScreen.this;
                    self.addRenderableOnly(
                            (guiGraphics, i, i1, v) -> guiGraphics.drawString(self.font,"Error",widgetX,widgetY,0xFFFF3737)
                    );
                }
            }
        }

        public void render(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY){
            guiGraphics.drawString(EndInvSettingScreen.this.font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
        }

        @SuppressWarnings("unchecked")
        public void syncConfig(){
            if(configWidget instanceof CycleButton<?> button){
                ((CycleButton<T>)button).setValue(configValue.get());
            }else if(configWidget instanceof EditBox editBox){
                assert configValue instanceof ModConfigSpec.IntValue;
                editBox.setValue(String.valueOf(configValue.get()));
            }
        }

        @Override
        public void applyChanges() {
            if(configWidget instanceof EditBox editBox){
                T parsed = parse(editBox.getValue());
                if(parsed==null) return;
                configValue.set(parsed);
            }
        }

        @Override
        public Optional<EditBox> getEditBox(){
            return configWidget instanceof EditBox box ? Optional.of(box) : Optional.empty();
        }

        @SuppressWarnings("unchecked")
        private T parse(String s){
            try{
                return (T)Integer.valueOf(s);
            }catch (Exception e){
                ((EditBox)configWidget).setValue(String.valueOf(configValue.get()));
                return null;
            }
        }

        public Component getTip() {
            return tip;
        }

        public ModConfigSpec.ConfigValue<?> getConfigValue() {
            return configValue;
        }
    }
}

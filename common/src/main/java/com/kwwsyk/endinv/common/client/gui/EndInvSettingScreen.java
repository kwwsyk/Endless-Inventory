package com.kwwsyk.endinv.common.client.gui;

import com.kwwsyk.endinv.common.client.option.ClientConfigs;
import com.kwwsyk.endinv.common.options.config.IConfigValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.kwwsyk.endinv.common.client.CachedSrcInv.INSTANCE;

/** A simple configuration screen for EndInv.
 *
 */
public abstract class EndInvSettingScreen extends Screen {

    //private static final Identifier BLANK_LOCATION = new Identifier("minecraft","textures/gui/demo_background.png");
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
    private final java.util.List<AbstractWidget> entryWidgets = new java.util.ArrayList<>();

    public final List<EntryBuilder> entries = new ArrayList<>();
    public final EntryBuilder[] renderingEntries = new EntryBuilder[7];
    @Nullable
    private EditBox typingEditBox;

    public EndInvSettingScreen(Screen lastScreen) {
        super(Component.translatable("title.endinv.settings"));
        this.back = lastScreen;
    }

    public static class Attachment extends EndInvSettingScreen{

        public Attachment(Screen lastScreen) {
            super(lastScreen);
        }

        protected void addConfigEntries(){
            addConfigEntry("endinv.setting.rows", ClientConfigs.ATTACHED_MENU_CONFIG.PageBasicLayout.Rows);
            // add auto rows toggle before auto columns
            addConfigEntry(Component.translatable("endinv.setting.auto_rows"), ClientConfigs.ATTACHED_MENU_CONFIG.PageBasicLayout.autoRows);
            addConfigEntry("endinv.setting.columns", ClientConfigs.ATTACHED_MENU_CONFIG.PageBasicLayout.Columns);
            addConfigEntry("endinv.setting.auto_suit", ClientConfigs.ATTACHED_MENU_CONFIG.PageBasicLayout.autoColumns);
            addConfigEntry("endinv.setting.attaching", ClientConfigs.DO_ATTACH);
            addConfigEntry("endinv.setting.texture", ClientConfigs.ATTACHED_MENU_CONFIG.TextureMode);
            addConfigEntry("endinv.setting.max_page_bar", ClientConfigs.ATTACHED_MENU_CONFIG.PageSwitchBar.MaxBars);
            addConfigEntry(Component.literal("Screen debug"), ClientConfigs.SCREEN_DEBUG);
        }
    }

    public static class Menu extends EndInvSettingScreen{
        public Menu(Screen lastScreen) {
            super(lastScreen);
        }

        protected void addConfigEntries(){
            addConfigEntry("endinv.setting.rows", ClientConfigs.EIM_CONFIG.Rows);
            addConfigEntry("endinv.setting.max_page_bar", ClientConfigs.EIM_CONFIG.PageSwitchBar.MaxBars);
            addConfigEntry(Component.literal("Screen debug"), ClientConfigs.SCREEN_DEBUG);
        }
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
            addConfigEntries();
        }
        // reset scroll when switching pages
        this.entryOffset = 0;
        this.scrollOffset = 0;
        scrollTo();
    }

    protected abstract void addConfigEntries();

    private void scrollTo(){
        // Remove previously created entry widgets to avoid duplication and stale positions
        if(!entryWidgets.isEmpty()){
            for (AbstractWidget w : entryWidgets) {
                this.removeWidget(w);
            }
            entryWidgets.clear();
        }
        int visible = Math.min(MAX_ENTRY_COUNT, entries.size());
        int maxStart = Math.max(0, entries.size() - visible);
        entryOffset = Mth.clamp(entryOffset, 0, maxStart);
        for(int i = 0; i<MAX_ENTRY_COUNT; ++i){
            int idx = i + entryOffset;
            renderingEntries[i] = (idx >= 0 && idx < entries.size()) ? entries.get(idx) : null;
        }
        for (int i = 0; i < MAX_ENTRY_COUNT; i++){
            var entry = renderingEntries[i];
            if(entry==null) continue;
            entry.placeAtSlot(i);
            entry.build();
            entry.syncConfig();
        }
    }

    private void pageSwitched(int index){
        this.pageIndex = index;
        switchPage();
    }

    protected  <T> void addConfigEntry(String key, IConfigValue<T> configValue){
        addConfigEntry(Component.translatable(key),configValue);
    }

    protected  <T> void addConfigEntry(Component tip, IConfigValue<T> configValue){
        entries.add(new ConfigEntry<>(entries.size(),tip,configValue.get(),configValue));
    }

    protected void addInfoEntry(Component tip, Supplier<Object> info){
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
    public void extractRenderState(@NotNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        // Render background first to respect 1.21.8 element ordering
        this.extractBackground(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
        super.extractRenderState(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
        for(var entry : renderingEntries){
            if(entry!=null) {
                entry.render(GuiGraphicsExtractor, partialTick, mouseX, mouseY);
            }
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0x88888888);
        
        //GuiGraphicsExtractor.blit(BLANK_LOCATION,leftPos,topPos,0,0,imageWidth,imageHeight);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.back);
    }

    // input handling is routed by the widget pipeline in 1.21.11

    @Override
    public void tick() {
        super.tick();
        boolean flag = this.getFocused() == null || !this.getFocused().isFocused() || this.getFocused() != typingEditBox;
        if(flag && this.typingEditBox !=null){
            for(var entry : entries){
                entry.getEditBox().ifPresent(editBox -> {
                    if(editBox== typingEditBox){
                        entry.applyChanges();
                        typingEditBox = null;
                    }
                });
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean p_434187_) {
        if(!super.mouseClicked(event, p_434187_)){
            if(getFocused() != null){
                getFocused().setFocused(false);
                setFocused(null);
            }
        }else {
            if(getFocused() != null && getFocused() instanceof EditBox editBox){
                typingEditBox = editBox;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(entries.size()>MAX_ENTRY_COUNT){
            // normalize wheel direction: negative scrollY means scrolling down
            // map to step-based offset change
            int maxStart = Math.max(0, entries.size()-MAX_ENTRY_COUNT);
            if (scrollY < 0) entryOffset = Math.min(entryOffset+1, maxStart);
            if (scrollY > 0) entryOffset = Math.max(entryOffset-1, 0);
            // also maintain a proportional indicator for potential future scrollbar
            this.scrollOffset = maxStart == 0 ? 0 : (double)entryOffset / (double)maxStart;
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

    public static void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, Component text, int minX, int minY, int maxX, int maxY, int color) {
        int i = font.width(text);
        int j = (minY + maxY - 9) / 2 + 1;
        int k = maxX - minX;
        if (i > k) {
            int l = i - k;
            double d0 = (double) Util.getMillis() / (double)1000.0F;
            double d1 = Math.max((double)l * (double)0.5F, 3.0F);
            double d2 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * d0 / d1)) / (double)2.0F + (double)0.5F;
            double d3 = Mth.lerp(d2, 0.0F, l);
            GuiGraphicsExtractor.enableScissor(minX, minY, maxX, maxY);
            GuiGraphicsExtractor.text(font, text, minX - (int)d3, j, color);
            GuiGraphicsExtractor.disableScissor();
        } else {
            GuiGraphicsExtractor.centeredText(font, text, (minX + maxX) / 2, j, color);
        }

    }

    public interface EntryBuilder{

        void build();

        void render(GuiGraphicsExtractor GuiGraphicsExtractor, float partialTick, int mouseX, int mouseY);

        void syncConfig();

        void applyChanges();

        default Optional<EditBox> getEditBox(){
            return Optional.empty();
        }

        // Position the entryâ€™s widget(s) at the visible slot index [0..MAX_ENTRY_COUNT)
        default void placeAtSlot(int slot){ }
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
        public void render(GuiGraphicsExtractor GuiGraphicsExtractor, float partialTick, int mouseX, int mouseY) {
            Font font = EndInvSettingScreen.this.font;
            GuiGraphicsExtractor.text(font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
            Component v = Component.literal(info.get()!=null? info.get().toString():"null");
            int infoLength = Math.min(font.width(v.getVisualOrderText()), 100);
            //GuiGraphicsExtractor.text(font,v,widgetMidX-infoLength/2,widgetY,0xFF00FFFF);
            renderScrollingString(GuiGraphicsExtractor,font,v,widgetMidX-infoLength/2,widgetY,widgetMidX+infoLength/2,widgetY+7,0xFF00FFFF);
        }

        @Override
        public void syncConfig() {
        }

        @Override
        public void applyChanges() {
        }

        @Override
        public void placeAtSlot(int slot) {
            widgetMidX = leftPos+WIDGET_X_OFFSET+WIDGET_X_SIZE/2 - 40;
            widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+slot*ENTRY_HEIGHT+1;
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
            entryWidgets.add(editBox);
        }

        @Override
        public void render(GuiGraphicsExtractor GuiGraphicsExtractor, float partialTick, int mouseX, int mouseY) {
            GuiGraphicsExtractor.text(EndInvSettingScreen.this.font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
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

        @Override
        public void placeAtSlot(int slot) {
            this.widgetX = leftPos+WIDGET_X_OFFSET;
            this.widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+slot*ENTRY_HEIGHT+1;
            if(this.editBox!=null){
                this.editBox.setX(widgetX);
                this.editBox.setY(widgetY);
            }
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
        private final IConfigValue<T> configValue;
        private final T initialValue;
        private AbstractWidget configWidget;
        int widgetX,widgetY;


        public ConfigEntry(int index, Component tip, T initialValue, IConfigValue<T> configValue){
            this.index = index;
            this.tip = tip;
            this.configValue = configValue;
            this.initialValue = initialValue;
            widgetX = leftPos+WIDGET_X_OFFSET;
            widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+index*ENTRY_HEIGHT+1;
        }

        @SuppressWarnings("unchecked")
        public void build() {
            switch (initialValue) {
                case Boolean b -> {
                    IConfigValue<Boolean> booleanValue = (IConfigValue<Boolean>) configValue;
                    var button = CycleButton.onOffBuilder(b)
                            .displayOnlyValue()
                            .create(widgetX, widgetY, WIDGET_X_SIZE, WIDGET_Y_SIZE, Component.empty(),
                                    (btn, value) -> booleanValue.set(value));
                    this.configWidget = button;
                    EndInvSettingScreen.this.addRenderableWidget(button);
                    entryWidgets.add(button);

                }
                case Enum<?> anEnum -> {
                    IConfigValue<Enum<?>> enumValue = (IConfigValue<Enum<?>>) configValue;
                    var button = new CycleButton.Builder<>(
                            (Enum<?> e) -> Component.translatable("endinv.setting.entry." + e.name()),
                            () -> anEnum)
                        .withValues((Enum<?>[]) initialValue.getClass().getEnumConstants())
                            .displayOnlyValue()
                            .create(widgetX, widgetY, WIDGET_X_SIZE, WIDGET_Y_SIZE, Component.empty(),
                                    (btn, value) -> enumValue.set(value));
                    this.configWidget = button;
                    EndInvSettingScreen.this.addRenderableWidget(button);
                    entryWidgets.add(button);

                }
                case Integer integer -> {
                    EditBox editBox = new EditBox(EndInvSettingScreen.this.font, widgetX, widgetY, WIDGET_X_SIZE, WIDGET_Y_SIZE, tip);
                    this.configWidget = editBox;
                    EndInvSettingScreen.this.addRenderableWidget(editBox);
                    entryWidgets.add(editBox);
                }
                case null, default -> {
                    EndInvSettingScreen self = EndInvSettingScreen.this;
                    self.addRenderableOnly((GuiGraphicsExtractor, i, i1, v) ->
                            GuiGraphicsExtractor.text(self.font, "Error", widgetX, widgetY, 0xFFFF3737));
                }
            }
        }

        public void render(GuiGraphicsExtractor GuiGraphicsExtractor, float partialTick, int mouseX, int mouseY){
            GuiGraphicsExtractor.text(EndInvSettingScreen.this.font,tip,leftPos+CONFIG_ENTRY_X_OFFSET,widgetY,0xFFFFFF00);
        }

        @SuppressWarnings("unchecked")
        public void syncConfig(){
            if(configWidget instanceof CycleButton<?> button){
                ((CycleButton<T>)button).setValue(configValue.get());
            }else if(configWidget instanceof EditBox editBox){
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

        public IConfigValue<?> getConfigValue() {
            return configValue;
        }

        @Override
        public void placeAtSlot(int slot) {
            this.widgetX = leftPos+WIDGET_X_OFFSET;
            this.widgetY = topPos+CONFIG_ENTRY_Y_OFFSET+slot*ENTRY_HEIGHT+1;
            if(this.configWidget!=null){
                this.configWidget.setX(widgetX);
                this.configWidget.setY(widgetY);
            }
        }
    }
}


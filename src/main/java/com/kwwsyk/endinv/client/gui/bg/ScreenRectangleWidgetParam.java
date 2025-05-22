package com.kwwsyk.endinv.client.gui.bg;

public record ScreenRectangleWidgetParam(int XPos,int YPos,int XSize,int YSize) {

    public boolean hasClickedOn(int mouseX, int mouseY){
        return mouseX>=XPos && mouseX<=XPos + XSize && mouseY>=YPos && mouseY<= YPos + YSize;
    }
}

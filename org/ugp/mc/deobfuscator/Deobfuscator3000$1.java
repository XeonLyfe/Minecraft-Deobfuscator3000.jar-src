package org.ugp.mc.deobfuscator;

import javafx.animation.AnimationTimer;

class Deobfuscator3000$1
extends AnimationTimer {
    Deobfuscator3000$1() {
    }

    @Override
    public void handle(long now) {
        Deobfuscator3000.this.mappingsPathBox.setDisable(Deobfuscator3000.this.isDeobfRunning || !Deobfuscator3000.this.shouldDeobf.isSelected());
        Deobfuscator3000.this.modPathBox.setDisable(Deobfuscator3000.this.isDeobfRunning);
        Deobfuscator3000.this.shouldDeobf.setDisable(Deobfuscator3000.this.isDeobfRunning);
        Deobfuscator3000.this.deobf.setText(Deobfuscator3000.this.deobfThread == null || !Deobfuscator3000.this.deobfThread.isAlive() ? (Deobfuscator3000.this.shouldDeobf.isSelected() ? "Deobfuscate" : "Decompile") : "Cancel");
        Deobfuscator3000.this.deobf.getTooltip().setText(!Deobfuscator3000.this.isDeobfRunning ? "Press Enter or click this button to deobfuscate your mod!" : "Press Enter or click this button to cancel running deobfuscation!");
    }
}

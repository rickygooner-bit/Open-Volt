package com.volt.module.modules.client;

import com.volt.Volt;
import com.volt.module.Category;
import com.volt.module.Module;

public class Panic extends Module {
  public Panic() {
    super("Panic", "Disables every module", -1, Category.CLIENT);
  }

  @Override
  public void onEnable() {
    for (Module m : Volt.INSTANCE.moduleManager.getModules()) {
      if (m.isEnabled() && m.getModuleCategory() != Category.CLIENT) {
        m.setEnabled(false);
      }
    }
  }
}

package com.isacofff.clientbase.modules;

import com.isacofff.clientbase.modules.features.ClickGui;
import com.isacofff.clientbase.modules.features.ExampleModule;
import com.isacofff.clientbase.modules.features.FullBright;
import com.isacofff.clientbase.Category;

import java.util.ArrayList;

public class Manager {

    public final ArrayList<Module> modules = new ArrayList<>();
    //Add the modules here for them to appear in the ClickGui
    public void init() {
    modules.add(new ClickGui());
    modules.add(new FullBright());
    modules.add(new ExampleModule());
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onUpdate();
            }
        }
    }

    public ArrayList<Module> getModules() {
        return modules;
    }

    public <T extends Module> T getModule(Class<T> classs) {

        for (Module m : modules) {
            if (classs.isInstance(m)) return classs.cast(m);
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public ArrayList<Module> getModulesByCategory(Category c) {
        ArrayList<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == c) list.add(m);
        }
        return list;
    }
}
 
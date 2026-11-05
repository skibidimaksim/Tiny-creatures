package com.wildkingdom;

import java.util.List;

public final class Species {
    public record Spec(String id, String arch, int ai, double hp, double dmg, double spd, double fspd,
                       String snd, float w, float h, String eff, String react) {}

    public static final List<Spec> ALL = List.of(
        new Spec("gazelle", "L", 0, 14.0, 2.0, 0.3, 0.0, "horse", 1.03f, 2.0f, null, null),
        new Spec("bison", "L", 1, 40.0, 6.0, 0.22, 0.0, "cow", 1.76f, 1.56f, null, null),
        new Spec("elephant", "L", 1, 90.0, 10.0, 0.2, 0.0, "cow", 1.7f, 2.2f, null, null),
        new Spec("moose", "L", 1, 40.0, 6.0, 0.24, 0.0, "horse", 1.5f, 2.64f, null, null),
        new Spec("grizzly", "L", 2, 45.0, 9.0, 0.26, 0.0, "polar", 1.4f, 1.31f, null, null),
        new Spec("tiger", "L", 2, 36.0, 8.0, 0.32, 0.0, "cat", 1.94f, 1.15f, null, null),
        new Spec("snow_leopard", "L", 2, 30.0, 7.0, 0.34, 0.0, "cat", 1.95f, 1.01f, null, null),
        new Spec("rhino", "L", 1, 70.0, 12.0, 0.24, 0.0, "cow", 1.5f, 1.7f, null, null),
        new Spec("raccoon", "L", 0, 12.0, 2.0, 0.3, 0.0, "fox", 1.2f, 0.56f, null, null),
        new Spec("skunk", "L", 0, 10.0, 1.0, 0.28, 0.0, "fox", 1.15f, 0.62f, null, "spray"),
        new Spec("tasmanian_devil", "L", 2, 16.0, 5.0, 0.32, 0.0, "fox", 0.99f, 0.56f, "weakness:100:0", null),
        new Spec("anteater", "L", 0, 22.0, 3.0, 0.24, 0.0, "cow", 2.19f, 0.88f, null, null),
        new Spec("kangaroo", "L", 1, 28.0, 6.0, 0.3, 0.0, "horse", 0.9f, 1.8f, null, null),
        new Spec("jerboa", "L", 0, 6.0, 1.0, 0.32, 0.0, "rabbit", 1.51f, 1.31f, null, null),
        new Spec("gorilla", "L", 2, 60.0, 10.0, 0.26, 0.0, "polar", 1.1f, 2.0f, null, null),
        new Spec("emu", "L", 1, 20.0, 4.0, 0.3, 0.0, "parrot", 0.8f, 1.9f, null, null),
        new Spec("seal", "L", 0, 24.0, 3.0, 0.2, 0.0, "dolphin", 1.0f, 0.7f, null, null),
        new Spec("crocodile", "L", 2, 44.0, 9.0, 0.22, 0.0, "spider", 1.1f, 0.7f, null, null),
        new Spec("caiman", "L", 2, 26.0, 6.0, 0.22, 0.0, "spider", 0.8f, 0.5f, null, null),
        new Spec("komodo", "L", 2, 34.0, 7.0, 0.26, 0.0, "spider", 0.9f, 0.6f, "poison:140:0", null),
        new Spec("anaconda", "L", 2, 34.0, 7.0, 0.22, 0.0, "spider", 0.9f, 0.5f, "slowness:80:1", null),
        new Spec("rattlesnake", "L", 2, 14.0, 3.0, 0.28, 0.0, "spider", 0.6f, 0.3f, "poison:160:0", null),
        new Spec("lobster", "L", 1, 12.0, 3.0, 0.2, 0.0, "spider", 0.7f, 0.5f, null, null),
        new Spec("seagull", "F", 0, 6.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.4f, null, null),
        new Spec("hummingbird", "F", 0, 4.0, 1.0, 0.25, 0.8, "bee", 0.3f, 0.3f, null, null),
        new Spec("toucan", "F", 0, 8.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.5f, null, null),
        new Spec("bald_eagle", "F", 2, 18.0, 5.0, 0.3, 0.7, "parrot", 0.8f, 0.7f, null, null),
        new Spec("crow", "F", 0, 6.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.5f, null, null),
        new Spec("catfish", "S", 1, 16.0, 3.0, 0.2, 0.0, "cod", 0.9f, 0.5f, null, null),
        new Spec("flying_fish", "S", 0, 6.0, 1.0, 0.3, 0.0, "cod", 0.5f, 0.4f, null, null),
        new Spec("moonfish", "S", 0, 6.0, 1.0, 0.25, 0.0, "cod", 0.5f, 0.6f, null, null),
        new Spec("hammerhead", "S", 2, 34.0, 7.0, 0.3, 0.0, "dolphin", 1.2f, 0.8f, null, null),
        new Spec("sperm_whale", "S", 1, 100.0, 12.0, 0.22, 0.0, "dolphin", 2.4f, 2.2f, null, null),
        new Spec("orca", "S", 2, 70.0, 10.0, 0.3, 0.0, "dolphin", 1.8f, 1.4f, null, null),
        new Spec("karapuzik", "L", 0, 6.0, 1.0, 0.24, 0.0, "silverfish", 0.6f, 0.4f, null, null),
        new Spec("kozyavka", "L", 0, 6.0, 1.0, 0.26, 0.0, "silverfish", 0.6f, 0.4f, null, null),
        new Spec("may_beetle", "L", 1, 8.0, 2.0, 0.22, 0.0, "silverfish", 0.6f, 0.5f, "poison:100:0", null),
        new Spec("pretendyashka", "L", 0, 6.0, 1.0, 0.22, 0.0, "silverfish", 0.5f, 0.4f, null, null),
        new Spec("cockroach", "L", 0, 6.0, 1.0, 0.3, 0.0, "silverfish", 0.6f, 0.3f, null, null),
        new Spec("car_cockroach", "L", 0, 8.0, 1.0, 0.28, 0.0, "silverfish", 0.6f, 0.3f, null, null),
        new Spec("fly", "F", 0, 4.0, 1.0, 0.25, 0.5, "bee", 0.4f, 0.4f, null, null),
        new Spec("chironomid", "F", 0, 3.0, 1.0, 0.25, 0.45, "bee", 0.4f, 0.5f, null, null),
        new Spec("fignya", "F", 0, 4.0, 1.0, 0.22, 0.4, "bee", 0.5f, 0.4f, null, null),
        new Spec("telifon", "L", 2, 16.0, 4.0, 0.26, 0.0, "spider", 0.8f, 0.4f, "poison:80:0", null),
        new Spec("strashila", "L", 2, 20.0, 5.0, 0.28, 0.0, "spider", 0.8f, 0.5f, "slowness:60:0", null)
    );

    private Species() {}
}

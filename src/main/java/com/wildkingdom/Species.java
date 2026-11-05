package com.wildkingdom;

import java.util.List;

public final class Species {
    public record Spec(String id, String arch, int ai, double hp, double dmg, double spd, double fspd,
                       String snd, float w, float h, String eff, String react, boolean ride, String prey) {}

    public static final List<Spec> ALL = List.of(
        new Spec("gazelle", "L", 0, 14.0, 2.0, 0.3, 0.0, "horse", 0.82f, 1.6f, null, null, true, ""),
        new Spec("bison", "L", 1, 40.0, 6.0, 0.22, 0.0, "cow", 2.25f, 2.0f, null, null, true, ""),
        new Spec("elephant", "L", 1, 90.0, 10.0, 0.2, 0.0, "cow", 3.0f, 3.0f, null, null, true, ""),
        new Spec("moose", "L", 1, 40.0, 6.0, 0.24, 0.0, "horse", 1.42f, 2.5f, null, null, true, ""),
        new Spec("grizzly", "L", 2, 45.0, 9.0, 0.26, 0.0, "polar", 1.81f, 1.7f, null, null, true, "catfish,raccoon,skunk"),
        new Spec("tiger", "L", 2, 36.0, 8.0, 0.32, 0.0, "cat", 2.19f, 1.3f, null, null, true, "gazelle,raccoon"),
        new Spec("snow_leopard", "L", 2, 30.0, 7.0, 0.34, 0.0, "cat", 2.11f, 1.1f, null, null, false, "gazelle,jerboa"),
        new Spec("rhino", "L", 1, 70.0, 12.0, 0.24, 0.0, "cow", 2.4f, 2.0f, null, null, true, ""),
        new Spec("raccoon", "L", 1, 12.0, 2.0, 0.3, 0.0, "fox", 0.96f, 0.45f, null, null, false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,lobster"),
        new Spec("skunk", "L", 1, 10.0, 1.0, 0.28, 0.0, "fox", 0.92f, 0.5f, null, "spray", false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,ant"),
        new Spec("tasmanian_devil", "L", 2, 16.0, 5.0, 0.32, 0.0, "fox", 0.88f, 0.5f, "weakness:100:0", null, false, "skunk,jerboa,karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach"),
        new Spec("anteater", "L", 1, 22.0, 3.0, 0.24, 0.0, "cow", 1.76f, 0.7f, null, null, false, "ant"),
        new Spec("kangaroo", "L", 1, 28.0, 6.0, 0.3, 0.0, "horse", 1.75f, 2.15f, null, null, true, ""),
        new Spec("jerboa", "L", 0, 6.0, 1.0, 0.32, 0.0, "rabbit", 1.21f, 1.05f, null, null, false, ""),
        new Spec("gorilla", "L", 2, 60.0, 10.0, 0.26, 0.0, "polar", 1.25f, 1.9f, null, null, false, ""),
        new Spec("emu", "L", 1, 20.0, 4.0, 0.3, 0.0, "parrot", 1.06f, 2.1f, null, null, true, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,ant"),
        new Spec("seal", "L", 1, 24.0, 3.0, 0.2, 0.0, "dolphin", 1.71f, 0.68f, null, null, false, "catfish,moonfish,flying_fish"),
        new Spec("crocodile", "L", 2, 44.0, 9.0, 0.22, 0.0, "spider", 4.0f, 0.63f, null, null, false, "gazelle,catfish"),
        new Spec("caiman", "L", 2, 26.0, 6.0, 0.22, 0.0, "spider", 3.61f, 0.54f, null, null, false, "catfish,jerboa,karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach"),
        new Spec("komodo", "L", 2, 34.0, 7.0, 0.26, 0.0, "spider", 3.61f, 0.54f, "poison:140:0", null, false, "gazelle,kangaroo,jerboa"),
        new Spec("anaconda", "L", 2, 34.0, 7.0, 0.22, 0.0, "spider", 3.04f, 0.5f, "slowness:80:1", null, false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,catfish,jerboa"),
        new Spec("rattlesnake", "L", 2, 14.0, 3.0, 0.28, 0.0, "spider", 1.76f, 0.5f, "poison:160:0", null, false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,jerboa,ant"),
        new Spec("lobster", "L", 1, 12.0, 3.0, 0.2, 0.0, "spider", 1.11f, 0.4f, null, null, false, ""),
        new Spec("seagull", "F", 1, 6.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.4f, null, null, false, "flying_fish,fly,chironomid,lobster"),
        new Spec("hummingbird", "F", 0, 4.0, 1.0, 0.25, 0.8, "bee", 0.3f, 0.3f, null, null, false, ""),
        new Spec("toucan", "F", 1, 8.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.5f, null, null, false, "fly,fignya,chironomid"),
        new Spec("bald_eagle", "F", 2, 18.0, 5.0, 0.3, 0.7, "parrot", 0.8f, 0.7f, null, null, false, "jerboa,crow,raccoon"),
        new Spec("crow", "F", 1, 6.0, 2.0, 0.25, 0.6, "parrot", 0.5f, 0.5f, null, null, false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,fly,ant"),
        new Spec("catfish", "S", 1, 16.0, 3.0, 0.2, 0.0, "cod", 1.54f, 0.65f, null, null, false, ""),
        new Spec("flying_fish", "S", 0, 6.0, 1.0, 0.3, 0.0, "cod", 1.31f, 0.53f, null, null, false, ""),
        new Spec("moonfish", "S", 0, 6.0, 1.0, 0.25, 0.0, "cod", 1.13f, 1.38f, null, null, false, ""),
        new Spec("hammerhead", "S", 2, 34.0, 7.0, 0.3, 0.0, "dolphin", 2.85f, 1.55f, null, null, false, "flying_fish,moonfish,catfish,seal"),
        new Spec("sperm_whale", "S", 1, 100.0, 12.0, 0.22, 0.0, "dolphin", 4.0f, 2.25f, null, null, false, ""),
        new Spec("orca", "S", 2, 70.0, 10.0, 0.3, 0.0, "dolphin", 3.8f, 2.43f, null, null, false, "seal,hammerhead,moonfish,flying_fish"),
        new Spec("karapuzik", "L", 0, 6.0, 1.0, 0.24, 0.0, "silverfish", 0.67f, 0.69f, null, null, false, ""),
        new Spec("kozyavka", "L", 0, 6.0, 1.0, 0.26, 0.0, "silverfish", 1.01f, 0.5f, null, null, false, ""),
        new Spec("may_beetle", "L", 1, 8.0, 2.0, 0.22, 0.0, "silverfish", 0.9f, 0.62f, "poison:100:0", null, false, ""),
        new Spec("pretendyashka", "L", 0, 6.0, 1.0, 0.22, 0.0, "silverfish", 0.71f, 0.62f, null, null, false, ""),
        new Spec("cockroach", "L", 0, 6.0, 1.0, 0.3, 0.0, "silverfish", 1.2f, 0.38f, null, null, false, ""),
        new Spec("car_cockroach", "L", 0, 8.0, 1.0, 0.28, 0.0, "silverfish", 1.09f, 0.44f, null, null, false, ""),
        new Spec("fly", "F", 0, 4.0, 1.0, 0.25, 0.5, "bee", 0.4f, 0.4f, null, null, false, ""),
        new Spec("chironomid", "F", 0, 3.0, 1.0, 0.25, 0.45, "bee", 0.4f, 0.5f, null, null, false, ""),
        new Spec("fignya", "F", 0, 4.0, 1.0, 0.22, 0.4, "bee", 0.5f, 0.4f, null, null, false, ""),
        new Spec("telifon", "L", 2, 16.0, 4.0, 0.26, 0.0, "spider", 1.2f, 0.38f, "poison:80:0", null, false, "karapuzik,kozyavka,may_beetle,pretendyashka,cockroach,car_cockroach,fly,ant,chironomid"),
        new Spec("strashila", "L", 2, 20.0, 5.0, 0.28, 0.0, "spider", 1.2f, 0.62f, "slowness:60:0", null, false, "ant,fly"),
        new Spec("ant", "L", 0, 3.0, 1.0, 0.3, 0.0, "silverfish", 0.71f, 0.56f, null, null, false, "")
    );

    private Species() {}
}

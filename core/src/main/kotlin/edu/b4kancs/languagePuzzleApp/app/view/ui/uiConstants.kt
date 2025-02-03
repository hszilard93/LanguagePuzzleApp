package edu.b4kancs.languagePuzzleApp.app.view.ui

val appInstructionsDescription =
    """
A feladatok a Feladatbank 2. kötete és a VI. osztályos tankönyvünk alapján készültek, de a papíron lévőkhöz képest nagyrész át van írva a szövegük, hogy ezen a platformon is megoldhatóak legyenek. Ettől függetlenül néha utasítunk, hogy nézz meg valamit a kötetben. Ilyenkor ide kell menned:
A Feladatbank2 - https://bit.ly/fb6oPuzzli
vagy ide:
A tankönyv - https://bit.ly/tk6oPuzzli
A gombokon lévő számok is ezeknek a köteteknek a lapszámait, valamint a feladatszámait jelölik.

Szómagyarázat:
  központi puzzle - ez legtöbbször egy igepuzzle, és szinte mindig lesznek fülei
  puzzle-darabok - ezek “öblös” puzzle-ek, és rácsatolhatók a központi puzzle fülére
  puzzle-szerkezet =  központi puzzle + puzzle-darabok

Használati útmutató:
  Amikor az egérmutatód egy puzzle fölött nyitott tenyérré változik, olyankor a bal egérbillentyű lenyomásával és nyomva tartásával “meg tudod fogni” és mozgatni tudod az adott puzzle-t. Ha egy egész puzzle-szerkezetet akarsz mozgatni, akkor azt a központi puzzle-től tudod “megfogni”. Ha a játéktéren a puzzle-darabokon kívül kattintasz a bal egérgombbal és tartod azt lenyomva, az egész játékteret tudod mozgatni.
  Görgetéssel, a laptopod érintőpadjával vagy az Alt+O/Alt+U billentyűkombinációkkal tudod nagyítani/kicsinyíteni a játékteret a puzzle-ekkel együtt. Az eredeti méretet az Alt+I billentyűkombinációval is vissza tudod állítani.

  Az egyes feladatokban más-más lehetőségeid lesznek a puzzle-ök használatára, ezek a következők lehetnek:
  A központi puzzle-re fület tenni úgy lehet (amikor a feladat típusa engedi), hogy a megfelelő helyre viszed az egeret, és amikor megjelenik a kék + jel, kattintasz. Ha egy már meglévő fülre viszed az egeret, akkor egy piros mínusz jelet látsz, rákattintva a fül törölhető.
  Ha az egérmutatót a fül fölé mozgatva egy fogaskereket látsz, akkor szintén egy bal kattintás után választhatsz színt a fülnek. Ez zöld, sárga vagy lila színű lehet.
  Amikor toldaléknak kell kerülnie a fülekre, akkor, azt vagy neked kell ráírni, vagy felajánlja a feladat, hogy válassz. Ha sárgát választottál, legtöbbször automatikusan megjelenik majd a tárgy “-t” toldaléka, ha lilát, akkor pedig egy legördülő listából választhatsz toldalékot. Bizonyos feladatok esetében nem csak toldalékot, hanem névutót vagy jelentéscímkét is választhatsz.
  A füleken kívül a központi puzzle-re és a  puzzle-darabokra is tudsz írni, ha a feladat úgy kéri. Ehhez duplán kell kattintanod az adott elem közepére, ahol az íráskurzor megjelenik. Ekkor megnyílik egy kis ablak, amibe írni lehet, majd elmenteni (vagy elvetni a szerkesztést). Mentés után a új vagy szerkesztett szöveg megjelenik azon az elemen, amelynek a közepére duplán kattintottál. Ha nagyon hosszú szöveget írsz be, előfordulhat, hogy a puzzle megnő, hogy helyet adjon a szövegnek. Fontos tudni, hogy egymáshoz csatolt puzzle-ek szövegeit nem szerkesztheted, ilyenkor előbb szét kell választanod őket.
  A puzzle-darabokat egy-egy bal kattintással tudod a megfelelő pozícióba forgatni olyankor, amikor látod fölöttük a nyitott tenyeret. Egy kattintásra 90 fokot fordulnak az óramutató járásával megegyező irányba.
  Olyan feladatokkal is találkozhatsz, ahol nincsenek eleve megadva puzzle-ek. Ilyenkor neked kell “lekérned” a megfelelő típusú (központi puzzle vagy puzzle-darab) és számú puzzle-t. Ilyenkor elérhető számodra a képernyő bal oldalán egy nagy zöld + gomb, erre kattintva kérhetsz új puzzle-t. Ha tévedtél, ilyenkor el is tudod dobni a puzzle-t a bal alsó sarokban megjelenő piros kukába húzva azt.
  A legtöbb feladatban zöld pipa fog megjelenni a képernyő jobb alsó sarkában, ha helyesen végezted el a feladatot. Egyes feladatoknak több megoldása is van. Amint az első jó megoldást megadod, megjelenik majd a zöld pipa, de attól próbálkozhatsz további megoldásokkal is, ha úgy gondolod, hogy vannak még ilyenek. Vannak esetek, amikor egymás megoldását kell ellenőriznetek, vagy a tanárodnak kell megmutatnod a megoldásod. Erre felhívjuk a figyelmed a feladat utasításában.
  “Lapozós” feladatokkal is találkozhatsz: ha a feladat egyik részét jól megoldottad (megjelent a zöld pipa), akkor mellette látni fogsz egy zöld >> (dupla nyíl) gombot. Erre kattintva előre lapozol a feladaton belül a következő részfeladatra. Ha anélkül akarsz továbbmenni, hogy sikeresen megoldottál volna egy részfeladatot, a Ctrl + jobbra/balra nyíl billentyűkombináció használatával is lapozhatsz előre/hátra.
  A feladatok bármely pontján vissza lehet térni a főmenübe, a feladatlistákhoz. Ehhez a képernyő jobb felső sarkában található visszafelé mutató nyíl gombra kell kattintani.
  Vigyázat, amint továbbmész egy oldalról, elvész az addigi munkád, a program nem jegyzi meg a egyes feladatok megoldásait!

  Ha sikeresen szeretnéd megoldani a feladatokat, először mindig alaposan olvasd el az utasítás szövegét! Figyelj arra, hogy mindent teljesítettél-e, amit a feladat kér! A helyesírásra is ügyelned kell ahhoz, hogy a program helyes megoldásként ismerje fel azt, amit létrehoztál. Akár egy fölösleges szóközön is múlhat a megoldásod helyessége. Ezeket mind ellenőrizd, ha nem kaptál zöld pipát!

Az  oktatási segédlet az MTA Domus Programja által támogatott tevékenység keretében jött létre. Pályázatvezető: Kádár Edit. Szerződésszám: 50/3/2024/HTMT
A segédlet felhasználási feltételeit a CC BY-NC-ND 4.0 licensz szabályozza.

Programozás: Hompoth Szilárd
A feladatokat adaptálta: Kádár Edit és Bartalis Boróka
""".trim()

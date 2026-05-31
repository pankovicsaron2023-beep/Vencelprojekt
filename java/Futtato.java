package kozlekedes;

import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// ==========================================
// 1. MODELL RÉTEG (ÜZLETI OBJEKTUMOK)
// ==========================================

class Megallo {
    private String nev;

    public Megallo(String nev) {
        this.nev = nev;
    }

    public String getNev() {
        return nev;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Megallo megallo = (Megallo) obj;
        return nev.equalsIgnoreCase(megallo.nev);
    }

    @Override
    public int hashCode() {
        return nev.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return nev;
    }
}

class Jarat {
    private String id;
    private String tipus;
    private List<Megallo> megallok;
    private List<LocalTime> indulasiIdok;
    private int kesesPerc;

    public Jarat(String id, String tipus) {
        this.id = id;
        this.tipus = tipus;
        this.megallok = new ArrayList<>();
        this.indulasiIdok = new ArrayList<>();
        this.kesesPerc = 0;
    }

    public String getId() { return id; }
    public String getTipus() { return tipus; }
    public List<Megallo> getMegallok() { return megallok; }
    public List<LocalTime> getIndulasiIdok() { return indulasiIdok; }
    public int getKesesPerc() { return kesesPerc; }
    public void setKesesPerc(int kesesPerc) { this.kesesPerc = kesesPerc; }

    public void megalloHozzaadas(Megallo megallo) {
        this.megallok.add(megallo);
    }

    public void indulasHozzaadas(LocalTime ido) {
        this.indulasiIdok.add(ido);
    }

    public boolean erintiAMegallot(String megalloNev) {
        for (Megallo m : megallok) {
            if (m.getNev().equalsIgnoreCase(megalloNev)) return true;
        }
        return false;
    }

    public int getMegalloIndex(String megalloNev) {
        for (int i = 0; i < megallok.size(); i++) {
            if (megallok.get(i).getNev().equalsIgnoreCase(megalloNev)) return i;
        }
        return -1;
    }
}

// ==========================================
// 2. ÜZLETI LOGIKA (CONTROLLER / MANAGER)
// ==========================================

class KozlekedesManager {
    private List<Jarat> jaratok;
    private List<Megallo> osszesMegallo;

    public KozlekedesManager() {
        this.jaratok = new ArrayList<>();
        this.osszesMegallo = new ArrayList<>();
    }

    public List<Jarat> getJaratok() { return jaratok; }
    
    public Megallo megalloKeresVagyLetrehoz(String nev) {
        for (Megallo m : osszesMegallo) {
            if (m.getNev().equalsIgnoreCase(nev)) return m;
        }
        Megallo uj = new Megallo(nev);
        osszesMegallo.add(uj);
        return uj;
    }

    public Jarat jaratKeres(String id) {
        for (Jarat j : jaratok) {
            if (j.getId().equalsIgnoreCase(id)) return j;
        }
        return null;
    }

    public void jaratHozzaadas(Jarat jarat) {
        this.jaratok.add(jarat);
    }

    public boolean kesesBeallitas(String jaratId, int perc) {
        Jarat j = jaratKeres(jaratId);
        if (j != null) {
            j.setKesesPerc(perc);
            return true;
        }
        return false;
    }

    public List<String> utvonalTervezes(String honnan, String hova) {
        List<String> eredmenyek = new ArrayList<>();
        for (Jarat j : jaratok) {
            int startIdx = j.getMegalloIndex(honnan);
            int vegIdx = j.getMegalloIndex(hova);
            
            if (startIdx != -1 && vegIdx != -1 && startIdx < vegIdx) {
                StringBuilder info = new StringBuilder("Járat: " + j.getTipus() + " " + j.getId() + " | Indulások tőled: ");
                for (LocalTime t : j.getIndulasiIdok()) {
                    LocalTime modositottIdo = t.plusMinutes(j.getKesesPerc());
                    info.append(modositottIdo).append(" (+").append(j.getKesesPerc()).append(" p késés) ");
                }
                eredmenyek.add(info.toString());
            }
        }
        return eredmenyek;
    }
}

// ==========================================
// 3. ADATKEZELŐ RÉTEG (FÁJLMENTÉS)
// ==========================================

class AdatKezelo {
    private static final String FAJL_NEV = "menetrend_adatok.txt";

    public static void adatokMentese(KozlekedesManager manager) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FAJL_NEV))) {
            for (Jarat j : manager.getJaratok()) {
                writer.println("JARAT;" + j.getId() + ";" + j.getTipus() + ";" + j.getKesesPerc());
                for (Megallo m : j.getMegallok()) {
                    writer.println("MEGALLO;" + m.getNev());
                }
                for (LocalTime t : j.getIndulasiIdok()) {
                    writer.println("IDO;" + t.toString());
                }
            }
            System.out.println("[Rendszer] Adatok elmentve: " + FAJL_NEV);
        } catch (IOException e) {
            System.out.println("[Hiba] Sikertelen mentés: " + e.getMessage());
        }
    }

    public static void adatokBetoltese(KozlekedesManager manager) {
        File file = new File(FAJL_NEV);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String sor;
            Jarat kivalasztottJarat = null;
            while ((sor = reader.readLine()) != null) {
                String[] darabok = sor.split(";");
                if (darabok[0].equals("JARAT")) {
                    kivalasztottJarat = new Jarat(darabok[1], darabok[2]);
                    kivalasztottJarat.setKesesPerc(Integer.parseInt(darabok[3]));
                    manager.jaratHozzaadas(kivalasztottJarat);
                } else if (darabok[0].equals("MEGALLO") && kivalasztottJarat != null) {
                    Megallo m = manager.megalloKeresVagyLetrehoz(darabok[1]);
                    kivalasztottJarat.megalloHozzaadas(m);
                } else if (darabok[0].equals("IDO") && kivalasztottJarat != null) {
                    kivalasztottJarat.indulasHozzaadas(LocalTime.parse(darabok[1]));
                }
            }
            System.out.println("[Rendszer] Adatok betöltve a fájlból.");
        } catch (Exception e) {
            System.out.println("[Hiba] Hiba a betöltéskor: " + e.getMessage());
        }
    }
}

class AlapAdatGenerator {
    public static void generalAlapAdatokat(KozlekedesManager manager) {
        if (!manager.getJaratok().isEmpty()) return;

        Jarat b7 = new Jarat("7", "Busz");
        b7.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Újpalota"));
        b7.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Keleti Pályaudvar"));
        b7.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Blaha Lujza tér"));
        b7.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Móricz Zsigmond körtér"));
        b7.indulasHozzaadas(LocalTime.of(7, 0));
        b7.indulasHozzaadas(LocalTime.of(7, 30));

        Jarat v46 = new Jarat("4-6", "Villamos");
        v46.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Széll Kálmán tér"));
        v46.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Oktogon"));
        v46.megalloHozzaadas(manager.megalloKeresVagyLetrehoz("Blaha Lujza tér"));
        v46.indulasHozzaadas(LocalTime.of(8, 0));

        manager.jaratHozzaadas(b7);
        manager.jaratHozzaadas(v46);
    }
}

// ==========================================
// 4. FELHASZNÁLÓI FELÜLET RÉTEG (CLI UI)
// ==========================================

class MenuRendszer {
    private KozlekedesManager manager;
    private Scanner scanner;

    public MenuRendszer(KozlekedesManager manager) {
        this.manager = manager;
        this.scanner = new Scanner(System.in);
    }

    public void indit() {
        int opcio = -1;
        while (opcio != 0) {
            menuKiiras();
            opcio = szamotBeolvas();
            menuVezerles(opcio);
        }
    }

    private void menuKiiras() {
        System.out.println("\n=== TÖMEGKÖZLEKEDÉSI RENDSER ===");
        System.out.println("1. Menetrend megtekintése járatok szerint");
        System.out.println("2. Menetrend megtekintése megállók szerint");
        System.out.println("3. Útvonaltervezés (átszállás nélkül)");
        System.out.println("4. Késés szimuláció (Admin)");
        System.out.println("5. Új járat felvétele (Admin)");
        System.out.println("0. Kilépés és mentés");
        System.out.print("Válasszon opciót: ");
    }

    private int szamotBeolvas() {
        while (!scanner.hasNextInt()) {
            System.out.println("[Hiba] Nem számot adott meg!");
            scanner.next();
            System.out.print("Válasszon opciót: ");
        }
        int szam = scanner.nextInt();
        scanner.nextLine(); 
        return szam;
    }

    private void menuVezerles(int opcio) {
        switch (opcio) {
            case 1:
                menetrendJaratSzerint();
                break;
            case 2:
                menetrendMegalloSzerint();
                break;
            case 3:
                utvonalTervezoMenu();
                break;
            case 4:
                kesesSzimulacioMenu();
                break;
            case 5:
                ujJaratMenu();
                break;
            case 0:
                System.out.println("Program leállítása...");
                break;
            default:
                System.out.println("Érvénytelen opció!");
                break;
        }
    }

    private void menetrendJaratSzerint() {
        System.out.println("\n--- JÁRATOK ---");
        for (Jarat j : manager.getJaratok()) {
            System.out.println(j.getTipus() + " " + j.getId() + " [Késés: " + j.getKesesPerc() + " perc]");
            System.out.print("  Megállók: ");
            for (int i = 0; i < j.getMegallok().size(); i++) {
                System.out.print(j.getMegallok().get(i) + (i < j.getMegallok().size() - 1 ? " -> " : ""));
            }
            System.out.print("\n  Indulások: ");
            for (LocalTime t : j.getIndulasiIdok()) {
                System.out.print(t.plusMinutes(j.getKesesPerc()) + "  ");
            }
            System.out.println("\n");
        }
    }

    private void menetrendMegalloSzerint() {
        System.out.print("Adja meg a megálló nevét: ");
        String megallo = scanner.nextLine();
        System.out.println("\n--- Érintő járatok ---");
        boolean van = false;
        for (Jarat j : manager.getJaratok()) {
            if (j.erintiAMegallot(megallo)) {
                System.out.println("- " + j.getTipus() + " " + j.getId());
                van = true;
            }
        }
        if (!van) System.out.println("Ehhez a megállóhoz nincs járat.");
    }

    private void utvonalTervezoMenu() {
        System.out.print("Induló megálló: ");
        String honnan = scanner.nextLine();
        System.out.print("Cél megálló: ");
        String hova = scanner.nextLine();

        List<String> lehetosegek = manager.utvonalTervezes(honnan, hova);
        System.out.println("\n--- TALÁLATOK ---");
        if (lehetosegek.isEmpty()) {
            System.out.println("Nincs közvetlen járat.");
        } else {
            lehetosegek.forEach(System.out.println);
        }
    }

    private void kesesSzimulacioMenu() {
        System.out.print("Járat száma (pl. 7): ");
        String id = scanner.nextLine();
        System.out.print("Késés (perc): ");
        int perc = szamotBeolvas();

        if (manager.kesesBeallitas(id, perc)) {
            System.out.println("[Sikeres] Késés rögzítve.");
        } else {
            System.out.println("[Hiba] Nem található ilyen járat.");
        }
    }

    private void ujJaratMenu() {
        System.out.print("Új járat száma: ");
        String id = scanner.nextLine();
        System.out.print("Típusa (Busz/Villamos): ");
        String tipus = scanner.nextLine();

        Jarat uj = new Jarat(id, tipus);
        while (true) {
            System.out.print("Megálló hozzáadása (vagy 'vege'): ");
            String mNev = scanner.nextLine();
            if (mNev.equalsIgnoreCase("vege")) break;
            uj.megalloHozzaadas(manager.megalloKeresVagyLetrehoz(mNev));
        }

        System.out.print("Alap indulási idő (óó:pp, pl. 14:20): ");
        try {
            uj.indulasHozzaadas(LocalTime.parse(scanner.nextLine()));
            manager.jaratHozzaadas(uj);
            System.out.println("[Sikeres] Járat elmentve.");
        } catch (Exception e) {
            System.out.println("[Hiba] Rossz formátum, a járat elvetve.");
        }
    }
}

// ==========================================
// 5. INDÍTÓ (MAIN)
// ==========================================

public class Futtato {
    public static void main(String[] args) {
        KozlekedesManager manager = new KozlekedesManager();
        AdatKezelo.adatokBetoltese(manager);
        AlapAdatGenerator.generalAlapAdatokat(manager);

        MenuRendszer menu = new MenuRendszer(manager);
        menu.indit();

        AdatKezelo.adatokMentese(manager);
    }
}
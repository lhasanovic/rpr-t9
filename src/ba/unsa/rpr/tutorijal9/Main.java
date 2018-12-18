package ba.unsa.rpr.tutorijal9;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static String ispisiGradove() throws SQLException {
        GeografijaDAO geografija = GeografijaDAO.getInstance();

        String string = "";
        for (Grad grad : geografija.gradovi()) {
            string += grad.getNaziv() + " (" + grad.getDrzava().getNaziv() + ") - " + grad.getBroj_stanovnika() + "\n";
        }

        return string;
    }

    public static void glavniGrad() throws SQLException {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Unesite naziv države");
        String drzava = keyboard.nextLine();

        Grad grad = GeografijaDAO.getInstance().glavniGrad(drzava);

        if (grad != null) {
            System.out.println("Glavni grad države " + drzava + " je " + grad.getNaziv());
        } else {
            System.out.println("Nepostojeća država");
        }
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("Gradovi su:\n" + ispisiGradove());
        glavniGrad();

    }
}

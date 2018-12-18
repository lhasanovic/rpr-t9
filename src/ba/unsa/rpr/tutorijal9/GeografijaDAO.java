package ba.unsa.rpr.tutorijal9;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class GeografijaDAO {
    private static GeografijaDAO instance = null;
    private Connection conn;

    private static GeografijaDAO initialize() throws SQLException {
        return instance = new GeografijaDAO();
    }

    private GeografijaDAO() throws SQLException {
        //ukoliko ne postoji trebamo insert date početne vrijednosti
        File file = new File("baza.db");
        boolean postoji = file.exists();

        //Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:baza.db");
        initializeTabele(); // ukoliko ne postoje, kreiraju se tabele grad i drzava

        if(!postoji) {
            insertGradove();
            insertDrzave();
        }
    }
    public static GeografijaDAO getInstance() throws SQLException {
        if (instance == null) initialize();
        return instance;
    }
    public static void removeInstance() {
        instance = null;
    }

    public Grad glavniGrad (String drzava) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select * from drzava where naziv=?");
        ps.setString(1, drzava);
        ResultSet result = ps.executeQuery();

        if(result.isClosed()) {
            return null;
        }
        int idDrzave = result.getInt("id");

        Drzava newDrzava = new Drzava();
        Grad grad = new Grad();

        newDrzava.setId(result.getInt("id"));
        newDrzava.setGlavni_grad(grad);
        newDrzava.setNaziv(result.getString("naziv"));

        ps = conn.prepareStatement("select * from grad where drzava=?");
        ps.setInt(1, idDrzave);
        result = ps.executeQuery();

        grad.setId(result.getInt("id"));
        grad.setNaziv(result.getString("naziv"));
        grad.setBroj_stanovnika(result.getInt("broj_stanovnika"));
        grad.setDrzava(newDrzava);

        return grad;
    }

    public void obrisiDrzavu (String drzava) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select id from drzava where naziv=?");
        ps.setString(1, drzava);
        ResultSet result = ps.executeQuery();

        if(result.isClosed()) {
            return;
        }
        int idDrzave = result.getInt("id");

        // obriši državu
        ps = conn.prepareStatement("delete from drzava where naziv=?");
        ps.setString(1, drzava);
        ps.executeUpdate();

        // obriši njene gradove
        ps = conn.prepareStatement("delete from grad where drzava=?");
        ps.setInt(1, idDrzave);
        ps.executeUpdate();
    }

    public ArrayList<Grad> gradovi() throws SQLException {
        ArrayList<Grad> gradovi = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("select * from grad order by broj_stanovnika desc");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Grad grad = new Grad();
            grad.setId(rs.getInt("id"));
            grad.setNaziv(rs.getString("naziv"));
            grad.setBroj_stanovnika(rs.getInt("broj_stanovnika"));
            // id drzave
            ps = conn.prepareStatement("select * from drzava where id=?");
            ps.setInt(1, rs.getInt("drzava"));
            ResultSet result = ps.executeQuery();
            grad.setDrzava(new Drzava(result.getInt("id"),result.getString("naziv"), grad));
            gradovi.add(grad);
        }
        return gradovi; // vraća spisak gradova sortiranih po broju stanovnika u opadajućem redoslijedu
    }

    public void dodajGrad (Grad grad) throws SQLException {
        int latestId = getLatestId("grad");

        PreparedStatement ps = conn.prepareStatement("select id from drzava where naziv=?");
        ps.setString(1, grad.getDrzava().getNaziv());
        ResultSet result = ps.executeQuery();

        ps = conn.prepareStatement("INSERT INTO grad(id,naziv,broj_stanovnika,drzava) VALUES(?,?,?,?)");

        ps.setInt(1, ++latestId);
        ps.setString(2, grad.getNaziv());
        ps.setInt(3,  grad.getBroj_stanovnika());
        if(!result.isClosed()) {
            ps.setInt(4, result.getInt("id"));
        }
        ps.executeUpdate();
    }

    public void dodajDrzavu (Drzava drzava) throws SQLException {
        int latestId = getLatestId("drzava");

        PreparedStatement ps = conn.prepareStatement("select id from grad where naziv=?");
        ps.setString(1, drzava.getGlavni_grad().getNaziv());
        ResultSet result = ps.executeQuery();

        if(result.isClosed()) {
            return; //vraća ako grad ne postoji
        }

        ps = conn.prepareStatement("INSERT INTO drzava(id,naziv,glavni_grad) VALUES(?,?,?)");

        ps.setInt(1, ++latestId);
        ps.setString(2, drzava.getNaziv());
        ps.setInt(3, result.getInt("id"));
        ps.executeUpdate();
    }

    public void izmijeniGrad (Grad grad) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("update grad set naziv=?,broj_stanovnika=?,drzava=? where id=?");
        ps.setString(1, grad.getNaziv());
        ps.setInt(2, grad.getBroj_stanovnika());
        ps.setInt(3, grad.getDrzava().getId());
        ps.setInt(4, grad.getId());
        ps.executeUpdate();
    }

    public Drzava nadjiDrzavu (String drzava) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select * from drzava where naziv=?");
        ps.setString(1, drzava);
        ResultSet result = ps.executeQuery();

        if(result.isClosed()) {
            return null;
        }

        Drzava newDrzava = new Drzava();
        Grad grad = new Grad();

        newDrzava.setId(result.getInt("id"));
        newDrzava.setNaziv(drzava);
        newDrzava.setGlavni_grad(grad);

        int glavniGrad = result.getInt("glavni_grad");


        ps = conn.prepareStatement("select * from grad where id=?");
        ps.setInt(1, glavniGrad);
        result = ps.executeQuery();

        grad.setId(result.getInt("id"));
        grad.setNaziv(result.getString("naziv"));
        grad.setBroj_stanovnika(result.getInt("broj_stanovnika"));
        grad.setDrzava(newDrzava);

        return newDrzava;
    }

    // custom funkcije zbog čitljivijeg koda
    private void initializeTabele() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS grad (\n"
                + "	id int PRIMARY KEY,\n"
                + "	naziv text,\n"
                + "	broj_stanovnika int,\n"
                + "     drzava int NULL,\n"
                + "     FOREIGN KEY (drzava) REFERENCES drzava(id)"
                + ");";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.execute();

        sql = "CREATE TABLE IF NOT EXISTS drzava (\n"
                + "	id int PRIMARY KEY,\n"
                + "	naziv text,\n"
                + "     glavni_grad int,\n"
                + "     FOREIGN KEY (glavni_grad) REFERENCES grad(id)"
                + ");";
        ps = conn.prepareStatement(sql);
        ps.execute();
    }

    private void insertGradove() throws SQLException {
        String sql = "INSERT INTO grad(id,naziv,broj_stanovnika,drzava) VALUES(?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, 1);
        ps.setString(2, "Pariz");
        ps.setInt(3,  2206488);
        ps.setInt(4, 1);
        ps.executeUpdate();

        ps.setInt(1, 2);
        ps.setString(2, "London");
        ps.setInt(3, 8825000);
        ps.setInt(4, 2);
        ps.executeUpdate();

        ps.setInt(1, 3);
        ps.setString(2, "Beč");
        ps.setInt(3, 1867582);
        ps.setInt(4, 3);
        ps.executeUpdate();

        ps.setInt(1, 4);
        ps.setString(2, "Mančester");
        ps.setInt(3, 545500);
        ps.setInt(4, 2);
        ps.executeUpdate();

        ps.setInt(1, 5);
        ps.setString(2, "Grac");
        ps.setInt(3, 325021);
        ps.setInt(4, 3);
        ps.executeUpdate();
    }

    private void insertDrzave() throws SQLException {
        String sql = "INSERT INTO drzava(id,naziv,glavni_grad) VALUES(?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, 1);
        ps.setString(2, "Francuska");
        ps.setInt(3,  1);
        ps.executeUpdate();

        ps.setInt(1, 2);
        ps.setString(2, "Engleska");
        ps.setInt(3,  2);
        ps.executeUpdate();

        ps.setInt(1, 3);
        ps.setString(2, "Austrija");
        ps.setInt(3,  3);
        ps.executeUpdate();
    }

    private int getLatestId(String tabela) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select id from " + tabela + " order by id desc");
        ResultSet result = ps.executeQuery();

        return result.getInt("id");
    }

    public void printBaze() throws SQLException {
        System.out.println("\nGRADOVI");

        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM grad");
        ResultSet rs = stmt.executeQuery();

        // loop through the result set
        while (rs.next()) {
            System.out.println(rs.getInt("id") +  "\t" +
                    rs.getString("naziv") + "\t" +
                    rs.getInt("broj_stanovnika") + "\t" +
                    rs.getInt("drzava"));
        }

        System.out.println("\nDRZAVE");

        stmt = conn.prepareStatement("select * from drzava");
        rs = stmt.executeQuery();

        // loop through the result set
        while (rs.next()) {
            System.out.println(rs.getInt("id") +  "\t" +
                    rs.getString("naziv") + "\t" +
                    rs.getInt("glavni_grad"));
        }
    }
}
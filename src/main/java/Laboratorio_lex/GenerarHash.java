package Laboratorio_lex;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenerarHash {
    public static void main(String[] args) {
        System.out.println("HASH_GENERADO=" + new BCryptPasswordEncoder().encode("Admin123!"));
    }
}

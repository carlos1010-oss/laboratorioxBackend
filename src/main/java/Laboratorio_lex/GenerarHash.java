package Laboratorio_lex;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Admin123! => " + encoder.encode("Admin123!"));
        System.out.println("senafactory* => " + encoder.encode("senafactory*"));
        System.out.println("admin123 => " + encoder.encode("admin123"));
    }
}

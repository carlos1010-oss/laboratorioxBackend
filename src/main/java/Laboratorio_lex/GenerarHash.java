package Laboratorio_lex;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        
        System.out.println("\n=================================================");
        System.out.println("HASH BCrypt generado para 'admin123':");
        System.out.println(hash);
        System.out.println("=================================================\n");
    }
}
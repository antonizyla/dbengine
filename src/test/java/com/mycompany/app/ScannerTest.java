package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ScannerTest {
    
    @Test
    public void testNumberParsing(){
        String a = "23";
        String b = "1223";
        String c = "0.0101";

        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().get(0).literal, 23.0);

        Scanner B = new Scanner(b);
        assertEquals(B.scanTokens().get(0).literal, 1223.0);

        Scanner C = new Scanner(c);
        assertEquals(C.scanTokens().get(0).literal, 0.0101);
    }

    @Test
    public void testNumbersFollowedByTokens(){
        String a = "22 >";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().size(), 2);
    }

    @Test
    public void testNumbersPrefixedByTokens(){
        String a = ") 22";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().size(), 2);
    }

    @Test
    public void testBeforeAndAfterTokens(){
        String a = ") 22 \"hello\" 4";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().size(), 4);
    }

    @Test 
    public void testStringScanning(){
        String a = "\"hello\"";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().get(0).literal, "hello");
    }

    @Test
    public void testIdentifier(){
        String a = "Hello";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().get(0).type, TokenType.IDENTIFIER);
    }

    @Test
    public void testImproperIdentifiers(){
        String a = "He3llo";
        Scanner A = new Scanner(a);
        assertEquals(A.scanTokens().size(), 3);
    }

    @Test
    public void testReserved(){
        String a = "Select from";
        Scanner A = new Scanner(a);
        var tokens = A.scanTokens();
        assertEquals(tokens.get(0).type, TokenType.SELECT);
        assertEquals(tokens.get(1).type, TokenType.FROM);
    }

}

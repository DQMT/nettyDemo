package netty;

public class Test {
    A a = new A();
    {
        a = new A();
    }
}

final class A {
    String a;

    A(){

    }
    A(String a) {
        a = a;
    }
}
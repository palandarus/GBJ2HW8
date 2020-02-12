package ru.gb.jtwo.lone.online;

public class Cat extends Animal {
    int age = 10;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    Cat(String name) {
        this.name = name;
    }

    @Override
    void voice() {
        super.voice();
        System.out.println("mrrrrrr");
    }

    void makeAMess() {
        System.out.println(name + "makes a mess");
    }
}

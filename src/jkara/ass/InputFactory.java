package jkara.ass;

import java.io.BufferedReader;
import java.io.IOException;

public interface InputFactory {

    BufferedReader open() throws IOException;
}

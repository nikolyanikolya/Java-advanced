package info.kgeorgiy.ja.Ignatov.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// :NOTE: не ловите RuntimeException
// :NOTE: разделите ошибки для input и output
// :NOTE: одно создание MessageDigest
public class Walker {
    private static byte[] calcSha1(Path input) throws WalkException {
        try (InputStream inputStream = Files.newInputStream(input)) {
            MessageDigest sha1;
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new WalkException("This algorithm of hashing is not available. " + e.getMessage());
            }
            byte[] block = new byte[4096];
            int length;
            while ((length = inputStream.read(block)) >= 0) {
                sha1.update(block, 0, length);
            }
            return sha1.digest();
        } catch (SecurityException e) {
            throw new WalkException("Can`t access the file for hashing. " + e.getMessage());
        } catch (UnsupportedOperationException e) {
            throw new WalkException("Some unsupported option is specified while processing the files for hashing. "
                    + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new WalkException("Invalid options are specified while processing the files for hashing. "
                    + e.getMessage());
        } catch (IOException e) {
            return null;
        }
    }

    public static void run(String[] args) throws WalkException {

        if (args == null || args.length < 2) {
            throw new WalkException("Not enough arguments");
        }
        if (args[0] == null || args[1] == null) {
            throw new WalkException("Some of arguments is null");
        }
        final Path output, input;
        try {
            output = Paths.get(args[1]);
            input = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            throw new WalkException("Invalid path of the input or output file. " + e.getMessage());
        }
        try {
            final Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new WalkException("Can`t create parent directories for the output file. " + e.getMessage());
        } catch (SecurityException e) {
            throw new WalkException("Access is denied during the creation of the output path. " + e.getMessage());
        }

        try (BufferedReader bufferedReader = Files.newBufferedReader(input)) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(output)) {
                String filename;
                while ((filename = bufferedReader.readLine()) != null) {
                    Path currentFile;
                    StringBuilder builder = new StringBuilder();
                    try {
                        currentFile = Paths.get(filename);
                        byte[] bytes = calcSha1(currentFile);
                        if (bytes != null) {
                            for (byte b : bytes) {
                                builder.append(String.format("%02x", b));
                            }
                        } else {
                            throw new IOException("Can`t process the file and get a hash");
                        }
                    } catch (InvalidPathException | IOException e) {
                        builder.append("0".repeat(40));
                    }
                    bufferedWriter.write(builder + " " + filename);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            } catch (IOException e) {
                throw new WalkException("Some problems occurred while processing the output or input file. " + e.getMessage());
            } catch (SecurityException e) {
                throw new WalkException("Access is denied during processing the output file" + e.getMessage());
            }
        } catch (IOException e) {
            throw new WalkException("Some problems occurred while processing the input file. " + e.getMessage());
        } catch (SecurityException e) {
            throw new WalkException("Access is denied during processing the input file" + e.getMessage());
        }
    }
}

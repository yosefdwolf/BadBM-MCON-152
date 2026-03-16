package edu.touro.mco152.bm;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for jDiskMark

 */
public class Util {

    static final DecimalFormat DF = new DecimalFormat("###.##");

    /**
     * Deletes the specified directory and all files within
     *
     * @param path Directory to delete
     * @return True if path deleted successfully
     */
    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return (path.delete());
    }


    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        return rand.nextInt((max - min) + 1) + min;
    }


    public static String displayString(double num) {
        return DF.format(num);
    }


    /**
     * Get OS specific disk info based on the drive the path is mapped to.
     *
     * @param dataDir the data directory being used in the run.
     * @return Disk info if available.
     */
    public static String getDiskInfo(File dataDir) {
        System.out.println("os: " + System.getProperty("os.name"));
        Path dataDirPath = Paths.get(dataDir.getAbsolutePath());
        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            // get disk info for linux
            String devicePath = Util.getDeviceFromPath(dataDirPath);
            String deviceModel = Util.getDeviceModel(devicePath);
            String deviceSize = Util.getDeviceSize(devicePath);
            return deviceModel + " (" + deviceSize + ")";
        } else if (osName.contains("Mac OS X")) {
            // get disk info for max os x
            String devicePath = Util.getDeviceFromPathOSX(dataDirPath);
            return Util.getDeviceModelOSX(devicePath);
        } else if (osName.contains("Windows")) {
            // get disk info for windows
            String driveLetter = dataDirPath.getRoot().toFile().toString().split(":")[0];
            return Util.getModelFromLetter2(driveLetter);
        }
        return "OS not supported";
    }

    /**
     * Get the drive model description based on the windows drive letter.
     * Uses the powershell script disk-model.ps1
     * <p>
     * Parses output such as the following:
     * <p>
     * DiskModel                          DriveLetter
     * ---------                          -----------
     * ST31500341AS ATA Device            D:
     * Samsung SSD 850 EVO 1TB ATA Device C:
     * <p>
     * Tested on Windows 10 on 3/6/2017
     *
     * @param driveLetter The windows drive letter whose model will be returned
     * @return A string describing disk model, or a default "Unknown" description
     */
    public static String getModelFromLetter2(String driveLetter) {
        try {
            Process p = Runtime.getRuntime().exec("powershell -ExecutionPolicy ByPass -File disk-model.ps1");
            p.waitFor(3, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();

            while (line != null) {
                System.out.println(line);
                if (line.trim().endsWith(driveLetter + ":")) {
                    //Typical Windows 10 sequence
                    String model = line.split(driveLetter + ":")[0];
                    System.out.println("model is: " + model);
                    return model.trim();
                } else if (line.trim().startsWith(driveLetter + ":")) {
                    // Windows 7 sequence
                    String model = line.split(driveLetter + ":")[1];
                    System.out.println("model is: " + model);
                    return model.trim();
                }

                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception in getModelFromLetter2: " + e.getMessage());
        }
        return "Unknown-Drive-Model for: " + driveLetter;
    }

    /**
     * On Linux OS get the device path when given a file path.
     * eg.  filePath = /home/james/Desktop/jDiskMarkData
     * devicePath = /dev/sda
     *
     * @param path the file path
     * @return the device path
     */
    static public String getDeviceFromPath(Path path) {
        try {
            Process p = Runtime.getRuntime().exec("df " + path.toString());
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            String curDevice;
            while (line != null) {
                //System.out.println(line);
                if (line.contains("/dev/")) {
                    curDevice = line.split(" ")[0];
                    // strip the partition digit if it is numeric
                    if (curDevice.substring(curDevice.length() - 1).matches("[0-9]{1}")) {
                        curDevice = curDevice.substring(0, curDevice.length() - 1);
                    }
                    return curDevice;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception getDeviceFromPath: " + e.getMessage());
        }
        return "Unknown-Device for: " + path;
    }


    /**
     * On Linux OS use the lsblk command to get the disk model number for a
     * specific Device i.e. /dev/sda
     *
     * @param devicePath path of the device
     * @return the disk model number
     */
    static public String getDeviceModel(String devicePath) {
        try {
            Process p = Runtime.getRuntime().exec("lsblk " + devicePath + " --output MODEL");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                //System.out.println(line);
                if (!line.equals("MODEL") && !line.trim().isEmpty()) {
                    return line;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception getDeviceModel: " + e.getMessage());
        }
        return "Unknown-Device-Model for: " + devicePath;
    }

    /**
     * On Linux OS use the lsblk command to get the disk size for a
     * specific Device i.e. /dev/sda
     *
     * @param devicePath path of the device
     * @return the size of the device
     */
    static public String getDeviceSize(String devicePath) {
        try {
            Process p = Runtime.getRuntime().exec("lsblk " + devicePath + " --output SIZE");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                //System.out.println(line);
                if (!line.contains("SIZE") && !line.trim().isEmpty()) {
                    return line;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception getDeviceSize: " + e.getMessage());
        }
        return "Unknown-Device-size for: " + devicePath;
    }

    static public String getDeviceFromPathOSX(Path path) {
        try {
            Process p = Runtime.getRuntime().exec("df " + path.toString());
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            String curDevice;
            while (line != null) {
                //System.out.println(line);
                if (line.contains("/dev/")) {
                    curDevice = line.split(" ")[0];
                    return curDevice;
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception getDeviceFromPathOSX: " + e.getMessage());
        }
        return "Unknown-Device for: " + path;
    }

    static public String getDeviceModelOSX(String devicePath) {
        try {
            String command = "diskutil info " + devicePath;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("Device / Media Name:")) {
                    return line.split("Device / Media Name:")[1].trim();
                }
                line = reader.readLine();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception getDeviceModelOSX: " + e.getMessage());
        }
        return "Generic " + devicePath;
    }
}

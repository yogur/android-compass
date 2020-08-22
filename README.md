# android-compass
A sample Android digital compass application that fuses accelerometer and magnetometer sensor data to obtain a tilt compensated heading. Moreover, a low-pass filter is used to obtain smoother sensor data, and magnetic declination is calculated to obtain true heading.

The following blog post discusses the sample application in detail: [Developing a Compass Android Application](https://talesofcode.com/calculate-heading-using-a-magnetometer-and-an-accelerometer/)

Attribution: The open source Java implementation of the World Magnetic Model (WMM) created by Los Alamos National Laboratory was used to calculate magnetic declination. [NOAA](https://www.ngdc.noaa.gov/geomag/WMM/thirdpartycontributions.shtml)

/* Copyright (c) 2021 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;

import static java.lang.Math.log;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/*
 * This file contains an example of a Linear "OpMode".
 * An OpMode is a 'program' that runs in either the autonomous or the teleop period of an FTC match.
 * The names of OpModes appear on the menu of the FTC Driver Station.
 * When a selection is made from the menu, the corresponding OpMode is executed.
 *
 * This particular OpMode illustrates driving a 4-motor Omni-Directional (or Holonomic) robot.
 * This code will work with either a Mecanum-Drive or an X-Drive train.
 * Both of these drives are illustrated at https://gm0.org/en/latest/docs/robot-design/drivetrains/holonomic.html
 * Note that a Mecanum drive must display an X roller-pattern when viewed from above.
 *
 * Also note that it is critical to set the correct rotation direction for each motor.  See details below.
 *
 * Holonomic drives provide the ability for the robot to move in three axes (directions) simultaneously.
 * Each motion axis is controlled by one Joystick axis.
 *
 * 1) Axial:    Driving forward and backward               Left-joystick Forward/Backward
 * 2) Lateral:  Strafing right and left                     Left-joystick Right and Left
 * 3) Yaw:      Rotating Clockwise and counter clockwise    Right-joystick Right and Left
 *
 * This code is written assuming that the right-side motors need to be reversed for the robot to drive forward.
 * When you first test your robot, if it moves backward when you push the left stick forward, then you must flip
 * the direction of all 4 motors (see code below).
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this OpMode to the Driver Station OpMode list
 */

@TeleOp(name="7935Teleop", group="Linear OpMode")
@Config
public class IntoTheDeepTeleop extends LinearOpMode {

    // Declare OpMode members for each of the 4 motors.
    private ElapsedTime runtime = new ElapsedTime();

    public static double MIN_DUMP = 0.0;
    public static double MAX_DUMP = 1.0;
    public static double MAX_LIFT = 1600;
    public static double MIN_LIFT = 345;
    public static double CLAMP_CLOSE = 0.05;
    public static double CLAMP_OPEN = 0.5;
    public static double SPECIMEN = 210;
    public static double MIN_ARM = 45;
    public static double ARM_GAIN = 0.005;
    public static double ARM_BREAK = -0.2;
    public static double ARM_FEEDFORWARD = 0.0;
    public static double DRIVE_CUT = 1.3;

    @Override
    public void runOpMode() {

        // Initialize the hardware variables. Note that the strings used here must correspond
        // to the names assigned during the robot configuration step on the DS or RC devices.
        DcMotor leftFrontDrive  = hardwareMap.get(DcMotor.class, "frontleft");
        DcMotor rightFrontDrive = hardwareMap.get(DcMotor.class, "frontright");
        DcMotor leftBackDrive  = hardwareMap.get(DcMotor.class, "backleft");
        DcMotor rightBackDrive = hardwareMap.get(DcMotor.class, "backright");
        DcMotor arm = hardwareMap.get(DcMotor.class, "arm");
        DcMotor lift = hardwareMap.get(DcMotor.class, "lift");
        Servo clamp = hardwareMap.get(Servo.class, "claw");
        Servo dump = hardwareMap.get(Servo.class, "dump");
        IMU imu = hardwareMap.get(IMU.class,"imu");
        DcMotor hang = hardwareMap.get(DcMotor.class,"hang");

        imu.initialize(
                new IMU.Parameters(new RevHubOrientationOnRobot(RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                        RevHubOrientationOnRobot.UsbFacingDirection.UP))
        );

        leftFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        leftBackDrive.setDirection(DcMotor.Direction.FORWARD);
        rightFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        rightBackDrive.setDirection(DcMotor.Direction.REVERSE);

        double liftstart = lift.getCurrentPosition();
        double armstart = arm.getCurrentPosition();

        boolean stop = false;

        // Wait for the game to start (driver presses START)
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        runtime.reset();

        int verticalOffset = 60;
        int horizontalOffset = 225;


        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {

            double max;

            // POV Mode uses left joystick to go forward & strafe, and right joystick to rotate.
            double axial   = gamepad1.left_stick_y/DRIVE_CUT;  // Note: pushing stick forward gives negative value
            double lateral = -gamepad1.left_stick_x;
            double yaw     =  gamepad1.right_stick_x/DRIVE_CUT;
            double liftpower = -gamepad2.right_stick_y;
            double armpower = gamepad2.left_stick_y;
            double hangpower = gamepad1.right_trigger - gamepad1.left_trigger;

            // Combine the joystick requests for each axis-motion to determine each wheel's power.
            // Set up a variable for each drive wheel to save the power level for telemetry.
            double leftFrontPower  = axial + lateral + yaw;
            double rightFrontPower = axial - lateral - yaw;
            double leftBackPower   = axial - lateral + yaw;
            double rightBackPower  = axial + lateral - yaw;

//            leftFrontPower  = gamepad1.x ? 1.0 : 0.0;  // X gamepad
//            leftBackPower   = gamepad1.a ? 1.0 : 0.0;  // A gamepad
//            rightFrontPower = gamepad1.y ? 1.0 : 0.0;  // Y gamepad
//            rightBackPower  = gamepad1.b ? 1.0 : 0.0;  // B gamepad

            // This ensures that the robot maintains the desired motion.
            max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
            max = Math.max(max, Math.abs(leftBackPower));
            max = Math.max(max, Math.abs(rightBackPower));

            if (max > 1.0) {
                leftFrontPower  /= max;
                rightFrontPower /= max;
                leftBackPower   /= max;
                rightBackPower  /= max;
            }

            // Send calculated power to wheels
            leftFrontDrive.setPower(leftFrontPower);
            rightFrontDrive.setPower(rightFrontPower);
            leftBackDrive.setPower(leftBackPower);
            rightBackDrive.setPower(rightBackPower);

            double liftPosition = lift.getCurrentPosition() - liftstart;
            double armPosition = arm.getCurrentPosition() - armstart;
            double dumpPos = MAX_DUMP - gamepad2.right_trigger*(MAX_DUMP-MIN_DUMP);
            double clampPos = CLAMP_OPEN - gamepad2.left_trigger*(CLAMP_OPEN-CLAMP_CLOSE);


            dump.setPosition(dumpPos);
            //clamp.setPosition(clampPos);

            if (gamepad2.left_bumper){
                clamp.setPosition(CLAMP_OPEN);
            }

            if (gamepad2.right_bumper){
                clamp.setPosition(CLAMP_CLOSE);
            }

            if (gamepad2.dpad_down){
                liftstart = lift.getCurrentPosition();
            }

            if (gamepad1.y){
                imu.resetYaw();
            }

//            if (gamepad2.dpad_right){
//                liftstart = lift.getCurrentPosition();
//                armstart = arm.getCurrentPosition();
//            }
//
//            if (gamepad2.dpad_up){
//                MIN_LIFT = liftPosition;
//            }
//
//            if (gamepad2.dpad_down){
//                MIN_ARM = armPosition;
//            }
            if (gamepad2.dpad_up){
                liftstart = lift.getCurrentPosition();
                armstart = arm.getCurrentPosition();
            }

            if (gamepad2.b){
                stop = false;
            }

            if (gamepad2.a){
                stop = true;
            }

            if (stop) {

                if (liftPosition < MIN_LIFT && liftpower < 0) {
                    liftpower = 0.1;
                }

                if (armPosition < MIN_ARM + 75 && armpower < 0) {
                    armpower *= ARM_GAIN * (armPosition-35);
                }

            }

            if (gamepad2.x){
                armpower += ARM_BREAK;
            }


            lift.setPower(liftpower);
            arm.setPower(armpower);
            hang.setPower(hangpower);

            // Show the elapsed game time and wheel power.
            telemetry.addData("Status", "Run Time: " + runtime.toString());
            telemetry.addData("Front left/Right", "%4.2f, %4.2f", leftFrontPower, rightFrontPower);
            telemetry.addData("Back  left/Right", "%4.2f, %4.2f", leftBackPower, rightBackPower);
            telemetry.addData("Back  left/Right", "%4.2f, %4.2f", leftBackPower, rightBackPower);
            telemetry.addData("Dump Pose", "%4.2f", dump.getPosition());
            telemetry.update();

            TelemetryPacket packet = new TelemetryPacket();
            packet.put("Arm Pose", arm.getCurrentPosition());
            packet.put("Lift Pose", lift.getCurrentPosition());
            packet.put("Lift Start", liftstart);
            packet.put("Dump Pose", dump.getPosition());
            packet.put("IMU", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
            packet.put("Stop", stop);
            FtcDashboard.getInstance().sendTelemetryPacket(packet);
        }
    }}

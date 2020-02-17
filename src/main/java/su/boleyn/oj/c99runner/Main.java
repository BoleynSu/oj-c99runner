package su.boleyn.oj.c99runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import su.boleyn.oj.judge.proto.Result;
import su.boleyn.oj.judge.proto.RunnerGrpc;
import su.boleyn.oj.judge.proto.Task;

public class Main extends RunnerGrpc.RunnerImplBase {
	private static final String RUNNER_ADDRESS = System.getenv().getOrDefault("RUNNER_ADDRESS", "0.0.0.0");
	private static final int RUNNER_PORT = Integer.parseInt(System.getenv().getOrDefault("RUNNER_PORT", "1993"));

	public Main() {
	}

	public static String read(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "utf-8"));
		StringBuffer data = new StringBuffer();
		char[] buffer = new char[1024];
		int length;
		while ((length = reader.read(buffer)) != -1) {
			data.append(String.valueOf(buffer, 0, length));
		}
		reader.close();
		return data.toString();
	}

	public void run(Task task, StreamObserver<Result> responseObserver) {
		Result.Builder builder = Result.newBuilder();
		try {
			String sourceFile = "/tmp/su.boleyn.oj-source.c";
			String inputFile = "/tmp/su.boleyn.oj-in.txt";
			String binaryFile = "/tmp/su.boleyn.oj-main";
			String outputFile = "/tmp/su.boleyn.oj-out.txt";
			try (PrintWriter out = new PrintWriter(sourceFile)) {
				out.write(task.getSource());
			}
			try (PrintWriter out = new PrintWriter(inputFile)) {
				out.write(task.getInput());
			}
			Runtime r = Runtime.getRuntime();
			if (r.exec(
					new String[] { "/bin/sh", "-c", "timeout 15 gcc -lm -std=c99 " + sourceFile + " -o " + binaryFile })
					.waitFor() != 0) {
				builder.setResult("compilation error").setOutput("").setTime(0).setMemory(0);
			} else {
				if (r.exec(new String[] { "/bin/sh", "-c",
						"timeout 5 " + binaryFile + " < " + inputFile + " > " + outputFile }).waitFor() != 124) {
					builder.setResult("accepted").setOutput(read(outputFile)).setTime(0).setMemory(0);
				} else {
					builder.setResult("time limit exceeded").setOutput("").setTime(5000).setMemory(0);
				}
			}
			new File(sourceFile).delete();
			new File(inputFile).delete();
			new File(binaryFile).delete();
			new File(outputFile).delete();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			builder.setResult("judge error").setOutput("").setTime(5000).setMemory(0);
		}
		responseObserver.onNext(builder.build());
		responseObserver.onCompleted();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("WARNING:");
		System.out.println(
				"The runner should be in a very restricted enviroment since we are running untrusted code in it.");
		Server server = NettyServerBuilder.forAddress(new InetSocketAddress(RUNNER_ADDRESS, RUNNER_PORT))
				.maxInboundMessageSize(100 * 1024 * 1024).addService(new Main()).build();
		server.start();
		server.awaitTermination();
	}
}

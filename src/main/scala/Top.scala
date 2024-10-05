package unittest

import freechips.rocketchip.unittest.UnitTest

trait VerilateTestHarness { this: emitrtl.Toplevel =>
  def dut: UnitTest

  override def topModule      = new TestHarness(dut)
  override def topModule_name = dut.getClass().getName().split("\\$").mkString(".")

  def CFLAGS(extra_flags: Seq[String]): Seq[String] = {
    val default = Seq("-std=c++17", "-DVERILATOR")
    val opts    = default ++ extra_flags
    opts.map(i => Seq("-CFLAGS", i)).flatten
  }

  def LDFLAGS(extra_flags: Seq[String]): Seq[String] = {
    val opts = extra_flags
    opts.map(i => Seq("-LDFLAGS", i)).flatten
  }

  def verilate(
    extra_CFLAGS:  Seq[String] = Seq(),
    extra_LDFLAGS: Seq[String] = Seq(),
    extras_src:    Seq[String] = Seq(),
  ) = {
    val cmd =
      Seq("verilator", "-Wno-LATCH", "-Wno-WIDTH", "--cc") ++ CFLAGS(extra_CFLAGS) ++ LDFLAGS(
        extra_LDFLAGS,
      ) ++
        extras_src ++
        Seq(
          "-f",
          "filelist.f",
          "--top-module",
          "TestHarness",
          "--trace",
          "--vpi",
          "--exe",
          s"${os.pwd.toString()}/src/main/resources/csrc/test_tb_top.cpp",
        )
    println(s"LOG: command invoked \"${cmd.mkString(" ")}\"")
    os.proc(cmd).call(cwd = os.Path(s"${os.pwd.toString()}/${out_dir}"), stdout = os.Inherit)
  }

  def build() = {
    val cmd = Seq("make", "-j", "-C", "obj_dir/", "-f", s"VTestHarness.mk")
    println(s"LOG: command invoked \"${cmd.mkString(" ")}\"")
    os.proc(cmd).call(cwd = os.Path(s"${os.pwd.toString()}/${out_dir}"), stdout = os.Inherit)
    println(s"VTestHarness executable in ./generated_sv_dir/${topModule_name}/obj_dir directory.")
  }
}

trait WithLazyModuleDUT { this: VerilateTestHarness with emitrtl.LazyToplevel =>
  override def dut            = lazyTop.module.asInstanceOf[UnitTest]
  override def topModule_name = lazyTop.getClass().getName().split("\\$").mkString(".")
}

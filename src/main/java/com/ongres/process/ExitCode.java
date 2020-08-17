/*-
 *  § 
 * fluent-process
 *    
 * Copyright (C) 2020 OnGres, Inc.
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * § §
 */

package com.ongres.process;

/**
 * This is an opinionated list of exit codes created starting from https://tldp.org/LDP/abs/html/exitcodes.html#AEN23647
 */
public class ExitCode {

  /**
   * Succesful termination.
   */
  public static final int OK = 0;

  /**
   * Catchall for general errors.
   * Example:
   * <pre>
   * let "var1 = 1/0"
   * </pre>
   * Miscellaneous errors, such as "divide by zero" and other impermissible operations.
   */
  public static final int ERROR = 1;

  /**
   * Command line usage error.
   */
  public static final int EX_USAGE = 64;
  /**
   * Data format error.
   */
  public static final int EX_DATAERR = 65;
  /**
   * Cannot open input.
   */
  public static final int EX_NOINPUT = 66;
  /**
   * Addressee unknown.
   */
  public static final int EX_NOUSER = 67;
  /**
   * Host name unknown.
   */
  public static final int EX_NOHOST = 68;
  /**
   * Service unavailable.
   */
  public static final int EX_UNAVAILABLE = 69;
  /**
   * Internal software error.
   */
  public static final int EX_SOFTWARE = 70;
  /**
   * System error (e.g., can't fork).
   */
  public static final int EX_OSERR = 71;
  /**
   * Critical OS file missing.
   */
  public static final int EX_OSFILE = 72;
  /**
   * Can't create (user) output file.
   */
  public static final int EX_CANTCREAT = 73;
  /**
   * Input/output error.
   */
  public static final int EX_IOERR = 74;
  /**
   * Temp failure; user is invited to retry.
   */
  public static final int EX_TEMPFAIL = 75;
  /**
   * Remote error in protocol.
   */
  public static final int EX_PROTOCOL = 76;
  /**
   * Permission denied.
   */
  public static final int EX_NOPERM = 77;
  /**
   * Configuration error.
   */
  public static final int EX_CONFIG = 78;

  /**
   *  Misuse of shell builtins (according to Bash documentation).
   *  Example:
   *  <pre>
   *    empty_function() {}
   *  </pre>
   *  Missing keyword or command, or permission problem (and diff return code on a failed binary
   *   file comparison).
   */
  public static final int SHELL_MISUSE = 2;

  /**
   * Command invoked cannot execute.
   * Example:
   * <pre>
   *   /dev/null
   * </pre>
   * Permission problem or command is not an executable.
   */
  public static final int CANNOT_EXECUTE = 126;

  /**
   * Command not found.
   * Example:
   * <pre>
   * illegal_command
   * </pre>
   * Possible problem with $PATH or a typo.
   */
  public static final int COMMAND_NOT_FOUND = 127;

  /**
   * Invalid argument to exit.
   * Example:
   * <pre>
   * exit 3.14159
   * </pre>
   * Exit takes only integer args in the range 0 - 255 (see first footnote).
   */
  public static final int INVALID_ARGUMENT = 128;

  /**
   * Fatal error signal "n".
   * <pre>
   * kill -9 $PPID of script
   * </pre>
   * $? returns 137 (128 + 9).
   */
  public static final int SIGHUP      = 129;
  public static final int SIGINT      = 130;
  public static final int SIGQUIT     = 131;
  public static final int SIGILL      = 132;
  public static final int SIGTRAP     = 133;
  public static final int SIGABRT     = 134;
  public static final int SIGBUS      = 135;
  public static final int SIGFPE      = 136;
  public static final int SIGKILL     = 137;
  public static final int SIGUSR1     = 138;
  public static final int SIGSEGV     = 139;
  public static final int SIGUSR2     = 140;
  public static final int SIGPIPE     = 141;
  public static final int SIGALRM     = 142;
  public static final int SIGTERM     = 143;
  public static final int SIGSTKFLT   = 144;
  public static final int SIGCHLD     = 145;
  public static final int SIGCONT     = 146;
  public static final int SIGSTOP     = 147;
  public static final int SIGTSTP     = 148;
  public static final int SIGTTIN     = 149;
  public static final int SIGTTOU     = 150;
  public static final int SIGURG      = 151;
  public static final int SIGXCPU     = 152;
  public static final int SIGXFSZ     = 153;
  public static final int SIGVTALRM   = 154;
  public static final int SIGPROF     = 155;
  public static final int SIGWINCH    = 156;
  public static final int SIGIO       = 157;
  public static final int SIGPWR      = 158;
  public static final int SIGSYS      = 159;
  public static final int SIGRTMIN    = 160;
  public static final int SIGRTMIN_1  = 161;
  public static final int SIGRTMIN_2  = 162;
  public static final int SIGRTMIN_3  = 163;
  public static final int SIGRTMIN_4  = 164;
  public static final int SIGRTMIN_5  = 165;
  public static final int SIGRTMIN_6  = 166;
  public static final int SIGRTMIN_7  = 167;
  public static final int SIGRTMIN_8  = 168;
  public static final int SIGRTMIN_9  = 169;
  public static final int SIGRTMIN_10 = 170;
  public static final int SIGRTMIN_11 = 171;
  public static final int SIGRTMIN_12 = 172;
  public static final int SIGRTMIN_13 = 173;
  public static final int SIGRTMIN_14 = 174;
  public static final int SIGRTMIN_15 = 175;
  public static final int SIGRTMAX_14 = 176;
  public static final int SIGRTMAX_13 = 177;
  public static final int SIGRTMAX_12 = 178;
  public static final int SIGRTMAX_11 = 179;
  public static final int SIGRTMAX_10 = 180;
  public static final int SIGRTMAX_9  = 181;
  public static final int SIGRTMAX_8  = 182;
  public static final int SIGRTMAX_7  = 183;
  public static final int SIGRTMAX_6  = 184;
  public static final int SIGRTMAX_5  = 185;
  public static final int SIGRTMAX_4  = 186;
  public static final int SIGRTMAX_3  = 187;
  public static final int SIGRTMAX_2  = 188;
  public static final int SIGRTMAX_1  = 189;
  public static final int SIGRTMAX    = 190;

  /**
   * Exit status out of range.
   * Example:
   * <pre>
   * exit -1
   * </pre>
   * exit takes only integer args in the range 0 - 255.
   */
  public static final int EXIT_OUT_OF_RANGE = 255;

  private ExitCode() {
  }
}

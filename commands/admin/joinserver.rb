# Command to join a server
# Use like "%admin/joinserver irc.xyz.net 6667 MewtwosNickOnTheServer"

require 'java'

bwt = Java::Meew0Mewtwo::BotWrapperThread.new(Java::Meew0Mewtwo::MewtwoMain.configuration, ARGV[4], ARGV[2], ARGV[3].to_i)
thread = java.lang.Thread.new(bwt)
thread.start

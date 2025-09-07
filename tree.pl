#!/usr/bin/perl
 
while (<STDIN>) {
    my $currLine = $_;
    my $numSlash = @{[$currLine =~ /(\/)/g]};
    my $lastSlash = rindex($currLine, '/');
    my $dirFile = substr($currLine,$lastSlash+1,length($currLine)-$lastSlash-2);
    if ($numSlash == 1) { print ".$numSlash /.\n"; }
    else    { print ".$numSlash $dirFile.\n"; }
}
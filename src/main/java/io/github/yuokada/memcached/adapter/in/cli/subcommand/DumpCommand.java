package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import io.github.yuokada.memcached.adapter.in.cli.EntryCommand;
import io.github.yuokada.memcached.application.usecase.DumpUseCase;
import java.util.List;
import java.util.concurrent.Callable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "dump")
public class DumpCommand implements Callable<Integer> {

    /*
     if ($mode eq 'dump') {
        print STDERR "Dumping memcache contents";
        print STDERR " (limiting to $limit keys)" unless !$limit;
        print STDERR "\n";
        print $sock "lru_crawler metadump all\r\n";
        my %keyexp;
        my $keycount = 0;
        while (<$sock>) {
            last if /^END/ or ($limit and $keycount == $limit);
            # return format looks like this
            # key=foo exp=2147483647 la=1521046038 cas=717111 fetch=no cls=13 size=1232
            if (/^key=(\S+) exp=(-?\d+) .* /) {
                my ($k, $exp) = ($1, $2);
                $k =~ s/%(.{2})/chr hex $1/eg;

                if ($exp == -1) {
                    $keyexp{$k} = 0;
                } else {
                    $keyexp{$k} = $exp;
                }
            }
            $keycount++;
        }

        if ($limit) {
            # Need to reopen the connection here to stop the metadump in
            # case the key limit was reached.
            #
            # XXX: Once a limit on # of keys returned is introduced in
            # `lru_crawler metadump`, this should be removed and the proper
            # parameter passed in the query above.
            close($sock);
            $sock = server_connect();
        }

        foreach my $k (keys(%keyexp)) {
            print $sock "get $k\r\n";
            my $response = <$sock>;
            if ($response =~ /VALUE (\S+) (\d+) (\d+)/) {
                my $flags = $2;
                my $len = $3;
                my $val;
                read $sock, $val, $len;
                print "add $k $flags $keyexp{$k} $len\r\n$val\r\n";
                # get the END
                $_ = <$sock>;
                $_ = <$sock>;
            }
        }
        exit;
    }
    */
    private static final Logger logger = LoggerFactory.getLogger(DumpCommand.class);
    @ParentCommand
    private EntryCommand entryCommand;
    @Inject
    DumpUseCase dumpUseCase;

    @Option(
        names = {"--limit"}, description = "Number of keys to dump. 0 is no limit.",
        defaultValue = "0"
    )
    int limit;

    private static final String message = "Dumping memcache contents";

    @Override
    public Integer call() {
        if (limit < 0) {
            System.err.println("Limit must be greater than or equal to 0");
            return ExitCode.USAGE;
        }

        if (limit > 0) {
            System.err.println(message.concat(String.format(" (limiting to %d keys)", limit)));
        } else {
            System.err.println(message);
        }

        try {
            List<DumpUseCase.DumpResult> results = dumpUseCase.execute(
                entryCommand.configEndpoint,
                entryCommand.clusterPort,
                limit
            );
            results.forEach(result -> {
                String value = result.value();
                String fmt = String.format("add %s 0 %d %d\n%s",
                    result.key(),
                    result.expiration(),
                    value.length(),
                    value
                );
                System.out.println(fmt);
            });
            return ExitCode.OK;
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }
}

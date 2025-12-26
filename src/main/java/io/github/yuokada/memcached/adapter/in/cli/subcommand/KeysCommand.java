package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import io.github.yuokada.memcached.application.usecase.KeysUseCase;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import org.jboss.logging.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Command(name = "keys")
public class KeysCommand implements Callable<Integer> {

    /*
    if ($mode eq 'keys') {
        print STDERR "Dumping memcache keys";
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
                print $_
            }
            $keycount++;
        }
        exit;
    }
    */

    private static final Logger logger = Logger.getLogger(KeysCommand.class);
    private static final String message = "Dumping memcache keys";
    @Inject
    KeysUseCase keysUseCase;
    @Option(
        names = {"--limit"}, description = "Number of keys to dump. 0 is no limit.",
        defaultValue = "0"
    )
    int limit;

    @Override
    public Integer call() {
        if (limit < 0) {
            System.err.println("Limit must be greater than or equal to 0");
            return ExitCode.USAGE;
        }

        if (limit > 0) {
            message.concat(String.format(" (limiting to %d keys)", limit));
        }
        System.out.println(message);

        try {
            List<String> keys = keysUseCase.execute(limit);
            keys.forEach(System.out::println);
            return ExitCode.OK;
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }
}

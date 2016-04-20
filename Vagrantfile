# coding: utf-8
# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "hashicorp/precise64"

  # The below is used w/ Otto to get a shared directory in the virtual
  # environment. It doesn't work.
  # See https://github.com/hashicorp/otto/issues/363
  # config.vm.synced_folder '{{ path.working }}', "/vagrant"

  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--audio", "coreaudio", "--audiocontroller", "ac97"]
  end

  config.vm.provision "shell", inline: <<-SHELL
    sudo apt-get update
    sudo apt-get install -y software-properties-common python-software-properties
    sudo apt-add-repository -y ppa:openjdk-r/ppa
    sudo apt-get update
    sudo debconf-set-selections <<< "jackd2 jackd/tweak_rt_limits boolean true"
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y alsa-utils jackd2 supercollider openjdk-8-jdk
    sudo wget -nv \
              -O /usr/local/bin/lein \
              https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
    sudo chmod a+x /usr/local/bin/lein
    sudo usermod -a -G audio vagrant
    amixer sset Master 100% unmute
    amixer sset PCM 100% unmute
    cat <<EOF | sudo tee /etc/dbus-1/system.d/org.freedesktop.ReserveDevice1.conf
<?xml version="1.0" encoding="UTF-8"?> <!-- -*- XML -*- -->

<!DOCTYPE busconfig PUBLIC
 "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
 "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
<busconfig>
  <policy group="audio">
    <allow own="org.freedesktop.ReserveDevice1.Audio0"/>
  </policy>
</busconfig>
EOF
    su vagrant -c 'pushd /vagrant && lein deps 2>/dev/null'
    sudo touch /var/log/jackd.log
    sudo chown vagrant /var/log/jackd.log
  SHELL

  config.vm.provision "shell", name: "env", privileged: false do |s|
    # Use the Host's env FORECAST_API_KEY for guest
    s.inline = "echo export #{ENV.assoc('FORECAST_API_KEY').join('=')} >> $HOME/.bashrc"
  end

  # Copy the Host's aws credentials file to the guest vm
  config.vm.provision "file", source: "~/.aws/credentials", destination: "~/.aws/credentials"

  config.vm.provision "shell", name: "jackd", run: "always", privileged: false do |s|
    # you need to restart the machine (w/ vagrant reload) in order for
    # this to run w/o incident. something about the dbus conf not
    # getting that reservedevice thing
    s.env = { 'DBUS_SESSION_BUS_ADDRESS' => 'unix:path=/run/dbus/system_bus_socket' }
    s.inline = 'nohup jackd -R -d alsa -r 44100 -P 0<&- &>/var/log/jackd.log &' # start with -d dummy w/o a soundcard
  end

end

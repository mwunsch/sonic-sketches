# coding: utf-8
# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://atlas.hashicorp.com/search.
  config.vm.box = "hashicorp/precise64"

  # Disable automatic box update checking. If you disable this, then
  # boxes will only be checked for updates when the user runs
  # `vagrant box outdated`. This is not recommended.
  # config.vm.box_check_update = false

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  # config.vm.network "private_network", ip: "192.168.33.10"

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:

  config.vm.provider "virtualbox" do |vb|
    vb.customize ["modifyvm", :id, "--audio", "coreaudio", "--audiocontroller", "ac97"]
  end

  # View the documentation for the provider you are using for more
  # information on available options.

  # Define a Vagrant Push strategy for pushing to Atlas. Other push strategies
  # such as FTP and Heroku are also available. See the documentation at
  # https://docs.vagrantup.com/v2/push/atlas.html for more information.
  # config.push.define "atlas" do |push|
  #   push.app = "YOUR_ATLAS_USERNAME/YOUR_APPLICATION_NAME"
  # end

  # Enable provisioning with a shell script. Additional provisioners such as
  # Puppet, Chef, Ansible, Salt, and Docker are also available. Please see the
  # documentation for more information about their specific syntax and use.
  config.vm.provision "shell", inline: <<-SHELL
    sudo apt-get update
    sudo apt-get install -y software-properties-common python-software-properties
    sudo apt-add-repository -y ppa:openjdk-r/ppa
    sudo apt-get update
    echo 'jackd2 jackd/tweak_rt_limits boolean true' | sudo debconf-set-selections
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

  config.vm.provision "shell", name: "jackd", run: "always", privileged: false do |s|
    # you need to restart the machine (w/ vagrant reload) in order for
    # this to run w/o incident. something about the dbus conf not
    # getting that reservedevice thing
    s.env = { 'DBUS_SESSION_BUS_ADDRESS' => 'unix:path=/run/dbus/system_bus_socket' }
    s.inline = 'nohup jackd -R -d alsa -r 44100 -P 0<&- &>/var/log/jackd.log &' # start with -d dummy w/o a soundcard
  end
end

# TODO:
# ✓ Need to start Jackd reliably
# + Need Forecast API key in ENV
# + Need AWS credentials profile for "sonic-sketch"

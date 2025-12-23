package com.ecommerece.project.service;

import com.ecommerece.project.exceptions.APIException;
import com.ecommerece.project.exceptions.ResourceNotFoundException;
import com.ecommerece.project.model.Address;
import com.ecommerece.project.model.User;
import com.ecommerece.project.payload.AddressDTO;
import com.ecommerece.project.repositories.AddressRepository;
import com.ecommerece.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl  implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address=modelMapper.map(addressDTO,Address.class);
        List<Address> addressList=user.getAddresses();
        if(addressList == null) {
            addressList = new ArrayList<>();
        }
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress=addressRepository.save(address);
        return modelMapper.map(savedAddress,AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAllAddresses() {
        List<Address> addresses=addressRepository.findAll();
        if(addresses.isEmpty()){
            throw new APIException("address list is empty");
        }
        List<AddressDTO> addressDTOs =addresses.stream()
                .map(address->modelMapper.map(address,AddressDTO.class)).toList();

        return addressDTOs;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("address","addressId",addressId));
        return modelMapper.map(address,AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList=user.getAddresses();
        if(addressList == null || addressList.isEmpty()){
            return new ArrayList<>();
        }
        List<AddressDTO> addressDTOs=addressList.stream()
                .map(address->modelMapper.map(address,AddressDTO.class)).toList();
        return addressDTOs;
    }

    @Override
    public AddressDTO updateAddressById(Long addressId,AddressDTO addressDTO) {
        Address addressfromdb=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("address","addressId",addressId));
//        Address address=modelMapper.map(addressDTO,Address.class);
        addressfromdb.setCity(addressDTO.getCity());
        addressfromdb.setCountry(addressDTO.getCountry());
        addressfromdb.setState(addressDTO.getState());
        addressfromdb.setStreet(addressDTO.getStreet());
        addressfromdb.setPincode(addressDTO.getPincode());
        addressfromdb.setBuildingName(addressDTO.getBuildingName());

        Address updatedAddress=addressRepository.save(addressfromdb);
        User user=addressfromdb.getUser();
        user.getAddresses().removeIf(address->address.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);
        return modelMapper.map(updatedAddress,AddressDTO.class);
    }

    @Override
    public String deleteAddressById(Long addressId) {
        Address address=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("address","addressId",addressId));
        User user=address.getUser();
        user.getAddresses().removeIf(add->add.getAddressId().equals(addressId));
        userRepository.save(user);
        addressRepository.delete(address);

        return " Address deleted successfully with AddressId: "+addressId;
    }
}